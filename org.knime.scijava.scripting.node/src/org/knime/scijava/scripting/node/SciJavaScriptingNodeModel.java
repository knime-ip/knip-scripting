package org.knime.scijava.scripting.node;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
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
import org.knime.scijava.commands.KNIMEExecutionService;
import org.knime.scijava.commands.adapter.OutputAdapter;
import org.knime.scijava.commands.adapter.OutputAdapterService;
import org.knime.scijava.commands.io.InputDataRowService;
import org.knime.scijava.commands.io.OutputDataRowService;
import org.knime.scijava.commands.settings.NodeModelSettingsService;
import org.knime.scijava.commands.simplemapping.SimpleColumnMappingService;
import org.knime.scijava.core.TempClassLoader;
import org.knime.scijava.scripting.base.CompileHelper;
import org.knime.scijava.scripting.base.CompileProductHelper;
import org.knime.scijava.scripting.base.ScriptingGateway;
import org.knime.scijava.scripting.node.settings.ColumnCreationMode;
import org.knime.scijava.scripting.node.settings.SciJavaScriptingNodeSettings;
import org.knime.scijava.scripting.node.settings.ScriptDialogMode;
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

	/* Node settings */
	private final SciJavaScriptingNodeSettings m_settings = new SciJavaScriptingNodeSettings();

	/* Service providing access to ScriptLanguages plugins */
	@Parameter
	private ScriptService m_scriptService;
	@Parameter
	private NodeModelSettingsService m_nodeModelSettingsService;
	@Parameter
	private SimpleColumnMappingService m_columnMappingService;
	@Parameter
	private KNIMEExecutionService m_executionService;
	@Parameter
	private OutputDataRowService m_outputrowService;
	@Parameter
	private InputDataRowService m_inputrowService;

	/* Current compiled command and its command info */
	private CompileProductHelper m_compileProduct;

	/*
	 * Compiler, which secretly manages special cases (and one day
	 * optimizations) for some languages
	 */
	private CompileHelper m_compiler;

	private final StringWriter m_errorWriter = new StringWriter();
	private final StringWriter m_outputWriter = new StringWriter();

	private ColumnRearranger m_colRearranger;

	/* DataTableSpec of the output data table, created from module outputs */
	private ScriptingCellFactory m_cellFactory;

	/* Output data table specification */
	private DataTableSpec m_outTableSpec = null;

	private String m_oldCode;

	// --- node lifecycle: configure/execute/reset ---

	/**
	 * @param context
	 *
	 */
	protected SciJavaScriptingNodeModel(final Context scijavaContext) {
		super(1, 1);

		m_context = scijavaContext;
		m_context.inject(this);

		try {
			m_compiler = new CompileHelper(scijavaContext, m_errorWriter,
					m_outputWriter);
		} catch (final IOException e) {
			getLogger().error(
					"Could not create temporary directory for Scripting Node.");
		}
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		final ScriptLanguage language = getCurrentLanguage();

		// only recompile if the code changed
		if (!m_settings.getScriptCode().equals(m_oldCode)) {
			m_oldCode = m_settings.getScriptCode();
			try {
				m_compileProduct = recompile(m_compiler,
						m_settings.getScriptCode(), language, m_errorWriter);

				m_cellFactory = new ScriptingCellFactory(m_context,
						m_compileProduct.createModule(language));

			} catch (final NullPointerException | ModuleException e) {
				LOGGER.error(e);
				// Throw exception to prevent node from being executed.
				// Warning: some script languages will not fail to compile
				// until executed.
				throw new InvalidSettingsException(
						"Code did not compile!, view log for more details.");
			}
		}

		// provide the input table spec to module preprocessors
		// (for column lookup in ColumnInputMappingKnimePreprocessor)
		m_inputrowService.setDataTableSpec(inSpecs[0]);

		// column creation mode
		if (m_settings
				.getColumnCreationMode() == ColumnCreationMode.APPEND_COLUMNS) {
			m_colRearranger = new ColumnRearranger(inSpecs[0]);
			m_colRearranger.append(m_cellFactory);
			m_outTableSpec = m_colRearranger.createSpec();
		} else {
			/* won't use the ColumnRearranger */
			m_outTableSpec = new DataTableSpec(m_cellFactory.getColumnSpecs());
		}

		return new DataTableSpec[] { m_outTableSpec };
	}

	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		// ensure the dialog was generated
		if (m_settings.getMode() != ScriptDialogMode.SETTINGS_EDIT) {
			throw new IllegalArgumentException(
					"Node is not correctly configured for execution! Please configure the node in the generated dialog!");
		}

		// prepare output table
		final BufferedDataTable inTable = inData[0];
		final BufferedDataContainer container = exec
				.createDataContainer(m_outTableSpec);
		BufferedDataTable out;

		// provide the KNIME data via Scijava services to module
		m_executionService.setExecutionContext(exec);

		// create a clean module
		final ScriptLanguage currentLanguage = getCurrentLanguage();
		m_compileProduct = recompile(m_compiler, m_settings.getScriptCode(),
				currentLanguage, m_errorWriter);
		m_cellFactory = new ScriptingCellFactory(m_context,
				m_compileProduct.createModule(currentLanguage));

		try (final TempClassLoader cl = new TempClassLoader(
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

		switch (m_settings.getColumnCreationMode()) {
		case APPEND_COLUMNS:
			return new RearrangingScriptingStreamableFunction(
					m_colRearranger.createStreamableFunction());
		case NEW_TABLE:
			return new ScriptingStreamableFunction();
		default:
			throw new IllegalArgumentException(
					"Setting: " + m_settings.getColumnCreationMode()
							+ " is not supported!");
		}
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
		m_settings.validateSettings(settings, m_nodeModelSettingsService);
		m_compileProduct = recompile(m_compiler, m_settings.getScriptCode(),
				getCurrentLanguage(), m_errorWriter);
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_settings.loadSettingsFrom(settings, m_nodeModelSettingsService,
				false);
		m_columnMappingService.deserialize(m_settings.getColumnInputMapping());

		getCurrentLanguage(); // ensure the language is still available
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_settings.setColumnInputMapping(m_columnMappingService.serialize());
		m_settings.saveSettingsTo(settings, m_nodeModelSettingsService);
	}

	/**
	 * @return {@link ScriptLanguage} with name
	 *         <code>m_settings.getScriptLanguageName()</code>.
	 */
	private ScriptLanguage getCurrentLanguage() {
		// NB Need to wrap this in the classloader to ensure that the language
		// plugins are detected correctly
		try (final TempClassLoader cl = new TempClassLoader(
				ScriptingGateway.get().createUrlClassLoader())) {
			final String languageName = m_settings.getScriptLanguageName();
			final ScriptLanguage language = m_scriptService
					.getLanguageByName(languageName);
			if (language == null) {
				throw new NullPointerException("Could not load language "
						+ languageName + " for Scripting Node.");
			}
			return language;
		}
	}

	private static CompileProductHelper recompile(final CompileHelper compiler,
			final String scriptCode, final ScriptLanguage language,
			StringWriter errorWriter) {

		try (final TempClassLoader cl = new TempClassLoader(
				ScriptingGateway.get().createUrlClassLoader())) {
			return compiler.compile(scriptCode, language);
		} catch (ScriptException e) {
			String error = errorWriter.toString();
			errorWriter.getBuffer().setLength(0);
			throw new IllegalArgumentException(
					"Script compilation failed: \n " + error);
		}

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
		private OutputAdapterService m_outAdapters;

		@Parameter
		private ModuleService m_moduleService;

		public ScriptingCellFactory(final Context context,
				final Module module) {
			m_module = module;
			setContext(context);
			m_spec = createDataColumnSpecs();
		}

		protected DataColumnSpec[] createDataColumnSpecs() {
			final List<DataColumnSpec> tableSpecs = new ArrayList<>();

			final String suffix = m_settings.getColumnSuffixModel().isEnabled()
					? m_settings.getColumnSuffix() : "";

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
			m_inputrowService.setInputDataRow(row);

			try {
				m_moduleService.run(m_module, true).get();
			} catch (InterruptedException | ExecutionException e) {
				throw new IllegalStateException(
						"Module execution failed in Row: " + row.getKey()
								+ ": \n" + " " + e);
			}

			final DataCell[] cells = m_outputrowService.getOutputDataCells();

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
		private TempClassLoader m_tempCl;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void init(final ExecutionContext exec) throws Exception {
			// provide the KNIME data via Scijava services to module
			m_executionService.setExecutionContext(exec);

			m_tempCl = new TempClassLoader(
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
			m_tempCl.close();
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
			m_colRearrangerFunction.init(exec);
		}

		/** {@inheritDoc} */
		@Override
		public final DataRow compute(final DataRow inputRow) {
			try {
				return m_colRearrangerFunction.compute(inputRow);
			} catch (final Exception e) {
				throw new IllegalArgumentException(
						"Exception caught while reading row "
								+ inputRow.getKey() + "! Caught exception "
								+ e);
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
