/*
 * $Id: ContrastStretchPane.java,v 1.1 2007/04/19 10:41:38 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.ActionLabel;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.ui.ImageInfoEditor;
import org.esa.beam.framework.ui.ImageInfoEditorModel;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.math.MathUtils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

class ImageInfoEditor2 extends ImageInfoEditor {
    final ColorManipulationForm parentForm;
    boolean showExtraInfo;

    public ImageInfoEditor2(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
        setLayout(new BorderLayout());
        setShowExtraInfo(true);
        addPropertyChangeListener("model", new ModelChangeHandler());
    }

    public boolean getShowExtraInfo() {
        return showExtraInfo;
    }

    public void setShowExtraInfo(boolean showExtraInfo) {
        boolean oldValue = this.showExtraInfo;
        if (oldValue != showExtraInfo) {
            this.showExtraInfo = showExtraInfo;
            updateStxOverlayComponent();
            firePropertyChange("showExtraInfo", oldValue, this.showExtraInfo);
        }
    }

    private void updateStxOverlayComponent() {
        removeAll();
        add(createStxOverlayComponent(), BorderLayout.NORTH);
        revalidate();
        repaint();
    }

    private JPanel createStxOverlayComponent() {
        JPanel stxOverlayComponent = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        stxOverlayComponent.setOpaque(false);
        stxOverlayComponent.setBorder(new EmptyBorder(4, 0, 0, 8));

        final ImageInfoEditorModel model = getModel();
        if (!showExtraInfo || model == null) {
            return stxOverlayComponent;
        }

        JComponent labels = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                final Rectangle bounds = getBounds();
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        };
        labels.setLayout(new GridBagLayout());
        labels.setBackground(new Color(255, 255, 255, 140));
        stxOverlayComponent.add(labels);

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        labels.add(new JLabel("Name: " + model.getParameterName()), gbc);
        gbc.gridy++;
        labels.add(new JLabel("Unit: " + model.getParameterUnit()), gbc);

        final Stx stx = model.getSampleStx();
        if (stx == null) {
            return stxOverlayComponent;
        }

        gbc.gridy++;
        labels.add(new JLabel("Min: " + MathUtils.round(model.getMinSample(), 1000.0)), gbc);
        gbc.gridy++;
        labels.add(new JLabel("Max: " + MathUtils.round(model.getMaxSample(), 1000.0)), gbc);
        if (stx.getResolutionLevel() > 0) {
            final ActionLabel label = new ActionLabel("Rough statistics!");
            label.setToolTipText("Click to compute accurate statistics.");
            label.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    askUser();
                }
            });
            gbc.gridy++;
            labels.add(label, gbc);
        }

        return stxOverlayComponent;
    }

    void askUser() {
        final int i = JOptionPane.showConfirmDialog(this,
                                                    "Compute accurate statistics?\n" +
                                                            "Note that this action may take some time.",
                                                    "Question",
                                                    JOptionPane.YES_NO_OPTION);
        if (i == JOptionPane.YES_OPTION) {
            SwingWorker sw = new StxComputer();
            sw.execute();
        }
    }


    private class ModelChangeHandler implements PropertyChangeListener, ChangeListener {


        public void stateChanged(ChangeEvent e) {
            updateStxOverlayComponent();
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (!"model".equals(evt.getPropertyName())) {
                return;
            }

            final ImageInfoEditorModel oldModel = (ImageInfoEditorModel) evt.getOldValue();
            final ImageInfoEditorModel newModel = (ImageInfoEditorModel) evt.getNewValue();
            if (oldModel != null) {
                oldModel.removeChangeListener(this);
            }
            if (newModel != null) {
                newModel.addChangeListener(this);
            }

            updateStxOverlayComponent();
        }
    }

    private class StxComputer extends ProgressMonitorSwingWorker {
        public StxComputer() {
            super(ImageInfoEditor2.this, "Computing statistics");
        }

        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            UIUtils.setRootFrameWaitCursor(ImageInfoEditor2.this);
            final ProductSceneView view = parentForm.getProductSceneView();
            if (view != null) {
                final RasterDataNode[] rasters = view.getRasters();
                try {
                    pm.beginTask("Computing statistics", rasters.length);
                    for (RasterDataNode raster : rasters) {
                        raster.getStx(true, SubProgressMonitor.create(pm, 1));
                    }
                } finally {
                    pm.done();
                }
            }
            return null;
        }

        @Override
        protected void done() {
            UIUtils.setRootFrameDefaultCursor(ImageInfoEditor2.this);
        }
    }
}