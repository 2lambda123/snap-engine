package org.esa.beam.gpf.common.reproject.ui;

import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

/**
 * Geographic collocation action.
 *
 * @author Ralf Quast
 * @version $Revision: 2535 $ $Date: 2008-07-09 14:10:01 +0200 (Mi, 09 Jul 2008) $
 */
public class OrthorectifyAction extends AbstractVisatAction {

    private ModelessDialog dialog;

    @Override
    public void actionPerformed(CommandEvent event) {
        if (dialog == null) {
            dialog = new ReprojectionDialog(true, "Orthorectify", "orthorectify", getAppContext());
        }
        dialog.show();
    }

}