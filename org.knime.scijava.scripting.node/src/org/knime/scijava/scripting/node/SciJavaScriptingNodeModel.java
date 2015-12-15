package org.knime.scijava.scripting.node;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.script.ScriptException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.scijava.commands.KNIMEScijavaContext;
import org.knime.scijava.commands.adapter.OutputAdapter;
import org.knime.scijava.commands.adapter.OutputAdapterService;
import org.knime.scijava.commands.mapping.process.KnimePostprocessor;
import org.knime.scijava.commands.mapping.process.KnimePreprocessor;
import org.knime.scijava.core.TempClassLoader;
import org.knime.scijava.scripting.base.CompileHelper;
import org.knime.scijava.scripting.base.CompileProductHelper;
import org.knime.scijava.scripting.base.ScriptingGateway;
import org.knime.scijava.scripting.node.settings.ColumnCreationMode;
import org.knime.scijava.scripting.node.settings.SciJavaScriptingNodeSettings;
import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleItem;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptService;

/**
 * NodeModel of the ScriptingNode
 *
 * @author Jonathan Hale (University of Konstanz)
 */
public class SciJavaScriptingNodeModel extends NodeModel {

	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(SciJavaScriptingNodeModel.class);

	/* scijava context stuff */
	private final Context m_context;
	private final KNIMEScijavaContext m_knimeContext;

	/* Node settings */
	private final SciJavaScriptingNodeSettings m_settings = new SciJavaScriptingNodeSettings();

	/* Service providing access to ScriptLanguages plugins */
	@Parameter
	private ScriptService m_scriptService;

	/* Current compiled command and its command info */
	private CompileProductHelper m_compileProduct;

	/*
	 * Compiler, which secretly manages special cases (and one day
	 * optimizations) for some languages
	 */
	private CompileHelper m_compiler;

	/*
	 * TODO Error and output writers which could once be used for directing
	 * script output.
	 */
	private final Writer m_errorWriter = new StringWriter();
	private final Writer m_outputWriter = new StringWriter();

	private ColumnRearranger m_colRearranger;

	/**
	 * @param context
	 * @param knimeContext
	 *
	 */
	protected SciJavaScriptingNodeModel(Context scijavaContext,
			KNIMEScijavaContext knimeContext) {
		super(1, 1);

		m_context = scijavaContext;
		m_knimeContext = knimeContext;

		// populate @Parameter members
		m_context.inject(this);

		// setup all required KNIME related Scijava services
		m_knimeContext.nodeModelSettings()
				.setSettingsModels(m_settings.otherSettings());

		try {
			m_compiler = new CompileHelper(scijavaContext);
		} catch (final IOException e) {
			getLogger().error(
					"Could not create temporary directory for Scripting Node.");
		}

		// error and output writers which could one day possibly be set to the
		// ScriptInfo.
		NodeLogger.addKNIMEConsoleWriter(m_errorWriter, NodeLogger.LEVEL.WARN,
				NodeLogger.LEVEL.ERROR);
		NodeLogger.addKNIMEConsoleWriter(m_outputWriter, NodeLogger.LEVEL.INFO,
				NodeLogger.LEVEL.DEBUG);
	}

	/* DataTableSpec of the output data table, created from module outputs */
	private ScriptingCellFactory m_cellFactory;

	/* Output data table specification */
	private DataTableSpec m_outTableSpec = null;

	/**
	 * @return {@link ScriptLanguage} with name
	 *         <code>m_settings.getScriptLanguageName()</code>.
	 */
	protected ScriptLanguage getCurrentLanguage() {
		final String languageName = m_settings.getScriptLanguageName();
		final ScriptLanguage language = m_scriptService
				.getLanguageByName(languageName);
		if (language == null) {
			throw new NullPointerException("Could not load language "
					+ languageName + " for Scripting Node.");
		}
		return language;
	}

	// --- node lifecycle: configure/execute/reset ---

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		try (final TempClassLoader cl = new TempClassLoader(
				ScriptingGateway.get().createUrlClassLoader())) {

			final ScriptLanguage language = getCurrentLanguage();
			try {
				m_compileProduct = m_compiler
						.compile(m_settings.getScriptCode(), language);

				m_cellFactory = new ScriptingCellFactory(m_context,
						m_compileProduct.createModule(language));

			} catch (final NullPointerException | ScriptException
					| ModuleException e) {
				LOGGER.error(e.getMessage());
				// Throw exception to prevent node from being executed.
				// Warning: some script languages will not fail to compile
				// until executed.
				throw new InvalidSettingsException(
						"Code did not compile!, view log for more details.");
			}

			// create SettingsModels for autogenerated settings
			createSettingsForCompileProduct();

			// provide the input table spec to module preprocessors
			// (for column lookup in ColumnInputMappingKnimePreprocessor)
			m_knimeContext.input().setDataTableSpec(inSpecs[0]);

			if (m_settings
					.getColumnCreationMode() == ColumnCreationMode.APPEND_COLUMNS) {
				m_colRearranger = new ColumnRearranger(inSpecs[0]);
				m_colRearranger.append(m_cellFactory);
				m_outTableSpec = m_colRearranger.createSpec();
			} else {
				/* won't use the ColumnRearranger */
				m_outTableSpec = new DataTableSpec(
						m_cellFactory.getColumnSpecs());
			}

			return new DataTableSpec[] { m_outTableSpec };
		}
	}

	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		// prepare output table
		final BufferedDataTable inTable = inData[0];
		final BufferedDataContainer container = exec
				.createDataContainer(m_outTableSpec);
		BufferedDataTable out;

		// provide the KNIME data via Scijava services to module
		m_knimeContext.execution().setExecutionContext(exec);

		try (final TempClassLoader tempCl = new TempClassLoader(
				ScriptingGateway.get().createUrlClassLoader())) {

			if (m_settings
					.getColumnCreationMode() == ColumnCreationMode.APPEND_COLUMNS) {
				out = exec.createColumnRearrangeTable(inTable, m_colRearranger,
						exec);
			} else { /* NEW_TABLE */
				for (final DataRow row : inTable) {
					final DataRow outputRow = new DefaultRow(row.getKey(),
							m_cellFactory.getCells(row));
					container.addRowToTable(outputRow);

					// check if user canceled execution of node
					exec.checkCanceled();
				}

				container.close();
				out = container.getTable();
			}
		}

		return new BufferedDataTable[] { out };
	}

	@Override
	protected void reset() {
		/* nothing to do */
	}

	// --- streaming ---

	@Override
	public StreamableOperator createStreamableOperator(
			final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
					throws InvalidSettingsException {

		if (m_settings
				.getColumnCreationMode() == ColumnCreationMode.APPEND_COLUMNS) {
			return new RearrangingScriptingStreamableFunction(
					m_colRearranger.createStreamableFunction());
		}
		return new ScriptingStreamableFunction();
	}

	@Override
	public InputPortRole[] getInputPortRoles() {
		return new InputPortRole[] { InputPortRole.DISTRIBUTED_STREAMABLE };
	}

	@Override
	public OutputPortRole[] getOutputPortRoles() {
		return new OutputPortRole[] { OutputPortRole.DISTRIBUTED };
	}

	// --- loading and saving ---

	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec)
					throws IOException, CanceledExecutionException {
		/* nothing to do */
	}

	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec)
					throws IOException, CanceledExecutionException {
		/* nothing to do */
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_settings.validateSettings(settings);
		/* nothing to do */
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_settings.loadSettingsFrom(settings);

		try (final TempClassLoader tempCl = new TempClassLoader(
				ScriptingGateway.get().createUrlClassLoader())) {
			 final ScriptLanguage lang = m_scriptService
			 .getLanguageByName(m_settings.getScriptLanguageName());
			 if (lang == null) {
			 getLogger()
			 .error("Language " + m_settings.getScriptLanguageName()
			 + " could not be found.");
			 return;
			 }

			 try {
			 m_compileProduct = m_compiler.compile(
			 m_settings.getScriptCode(), getCurrentLanguage());
			 } catch (NullPointerException | ScriptException e) {
			 // compilation failed
			 getLogger().info(
			 "Code did not compile, failed to load all settings.");
			 return;
			 }

			// load column input mappings
			m_knimeContext.inputMapping().clear();
			m_knimeContext.inputMapping()
					.deserialize(m_settings.getColumnInputMapping());

			createSettingsForCompileProduct();
			m_knimeContext.nodeDialogSettings().loadSettingsFrom(settings,
					true);
		}
	}

	private void createSettingsForCompileProduct() {
		// Create settings models for module inputs which do not have a
		// ColumnToModuleInputMapping that maps to them
		for (final ModuleItem<?> i : m_compileProduct.inputs()) {
			final String inputName = i.getName();

			// unmapped inputs need an ui element.
			boolean needsUI = m_knimeContext.inputMapping()
					.isInputMapped(inputName);

			if (needsUI) {
				m_knimeContext.nodeModelSettings().createAndAddSettingsModel(i);
			}
		}
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		// store the inputmapping into the settings model
		m_settings.getColumnInputMappingModel()
				.setStringArrayValue(m_knimeContext.inputMapping().serialize());
		m_settings.saveSettingsTo(settings);

		// try (final TempClassLoader tempCl = new TempClassLoader(
		// ScriptingGateway.get().createUrlClassLoader())) {
		// m_compileProduct = m_compiler.compile(m_settings.getScriptCode(),
		// getCurrentLanguage());
		//
		// createSettingsForCompileProduct();
		// m_knimeContext.nodeModelSettings().saveSettingsTo(settings);
		//
		// } catch (final ScriptException e) {
		// // Compilation failure
		// return;
		// }
	}

	// --- nested classes ---

	/**
	 * CellFactory for ScriptingNode.
	 *
	 * @author Jonathan Hale
	 */
	protected class ScriptingCellFactory extends AbstractContextual
			implements CellFactory {

		private final Module m_module;
		private final DataColumnSpec[] m_spec;

		@Parameter
		OutputAdapterService m_outAdapters;

		@Parameter
		ModuleService m_moduleService;

		public ScriptingCellFactory(Context context, Module module) {
			m_module = module;
			setContext(context);
			m_spec = createDataColumnSpecs();
		}

		protected DataColumnSpec[] createDataColumnSpecs() {
			final ArrayList<DataColumnSpec> tableSpecs = new ArrayList<>();

			final String suffix = (m_settings.getColumnSuffixModel()
					.isEnabled()) ? m_settings.getColumnSuffix() : "";

			for (final ModuleItem<?> output : m_module.getInfo().outputs()) {
				@SuppressWarnings("unchecked")
				final OutputAdapter<?, DataCell> outputAdapter = m_outAdapters
						.getMatchingOutputAdapter(output.getType());
				final DataType type;

				if (outputAdapter != null) {
					type = DataType.getType(outputAdapter.getOutputType());
				} else {
					// print warning if output is not the special scijava script
					// module return value.
					if (output.getName() != "result"
							&& output.getType() != Object.class) {
						getLogger()
								.warn("Could not find an OutputAdapter for \""
										+ output.getName() + "\", skipping.");
					}
					continue;
				}

				// Add the column to table specs with name equal to the outputs
				// name with suffix.
				tableSpecs.add(new DataColumnSpecCreator(
						output.getName() + suffix, type).createSpec());
			}

			return tableSpecs.toArray(new DataColumnSpec[] {});
		}

		@Override
		public DataCell[] getCells(final DataRow row) {
			m_knimeContext.input().setInputDataRow(row);

			try {
				m_moduleService.run(m_module, true).get();
			} catch (InterruptedException | ExecutionException e) {
				getLogger().error(
						"Module execution failed in Row: " + row.getKey());
			}

			DataCell[] cells = m_knimeContext.output().getOutputDataCells();

			m_compileProduct.resetModule(m_module);

			return cells;
		}

		@Override
		public DataColumnSpec[] getColumnSpecs() {
			return m_spec;
		}

		@Override
		public void setProgress(final int curRowNr, final int rowCount,
				final RowKey lastKey, final ExecutionMonitor exec) {
			/* How does this help the cause? */
		}

		protected void resetModule() {
			m_compileProduct.resetModule(m_module);
		}
	}

	// --- streamable functions ---

	/**
	 * Streamable function for ScriptingNode.
	 *
	 * @author Jonathan Hale
	 */
	protected class ScriptingStreamableFunction extends StreamableFunction {
		private TempClassLoader tempCl;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void init(final ExecutionContext exec) throws Exception {
			// provide the KNIME data via Scijava services to module
			m_knimeContext.execution().setExecutionContext(exec);

			tempCl = new TempClassLoader(
					ScriptingGateway.get().createUrlClassLoader());
		}

		@Override
		public DataRow compute(final DataRow input) throws Exception {
			return new DefaultRow(input.getKey(),
					m_cellFactory.getCells(input));
		}

		@Override
		public void finish() {
			super.finish();

			tempCl.close();
		}
	}

	/**
	 * Streamable function for ScriptingNode using a column rearranger.
	 *
	 * @author Jonathan Hale
	 */
	protected class RearrangingScriptingStreamableFunction
			extends StreamableFunction {

		final StreamableFunction m_colRearrangerFunction;

		/**
		 * Constructor
		 *
		 * @param streamableFunction
		 *            Function of the column rearranger
		 */
		public RearrangingScriptingStreamableFunction(
				final StreamableFunction streamableFunction) {
			m_colRearrangerFunction = streamableFunction;
		}

		/** {@inheritDoc} */
		@Override
		public final void init(final ExecutionContext exec) throws Exception {
			super.init(exec);
			m_colRearrangerFunction.init(exec);
		}

		/** {@inheritDoc} */
		@Override
		public final DataRow compute(final DataRow inputRow) {
			try {
				return m_colRearrangerFunction.compute(inputRow);
			} catch (Exception e) {
				throw new IllegalArgumentException(
						"Exception caught while reading row "
								+ inputRow.getKey() + "! Caught exception "
								+ e.getMessage());
			}
		}

		/** {@inheritDoc} */
		@Override
		public final void finish() {
			m_colRearrangerFunction.finish();
			super.finish();
		}

		/** {@inheritDoc} */
		@Override
		public final StreamableOperatorInternals saveInternals() {
			return m_colRearrangerFunction.saveInternals();
		}
	};

}
