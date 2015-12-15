/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.scijava.scripting.node;


import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.scijava.commands.DefaultKNIMEScijavaContext;
import org.knime.scijava.scripting.base.ScriptingGateway;
import org.knime.scijava.scripting.node.ui.SciJavaScriptingNodeDialog;
import org.scijava.Context;

/**
 * NodeFactory for {@link SciJavaScriptingNodeModel}.
 *
 * Creates {@link SciJavaScriptingNodeModel}s and {@link SciJavaScriptingNodeDialog}s.
 *
 * @author <a href="mailto:jonathan.hale@uni-konstanz.de">Jonathan Hale</a>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 */
public class SciJavaScriptingNodeFactory extends NodeFactory<SciJavaScriptingNodeModel> {

	

	private Context m_context;
	private DefaultKNIMEScijavaContext m_knimeContext;

	public SciJavaScriptingNodeFactory() {
		super();
		m_context = ScriptingGateway.get().createSubContext();
		m_knimeContext = new DefaultKNIMEScijavaContext();
		m_knimeContext.setContext(m_context);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return null (ScriptingNode does not have views.)
	 */
	@Override
	public NodeView<SciJavaScriptingNodeModel> createNodeView(final int viewIndex,
			final SciJavaScriptingNodeModel nodeModel) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean hasDialog() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new SciJavaScriptingNodeDialog(m_context, m_knimeContext);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SciJavaScriptingNodeModel createNodeModel() {
		return new SciJavaScriptingNodeModel(m_context, m_knimeContext);
	}

}
