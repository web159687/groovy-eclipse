/*
 * Copyright 2009-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.eclipse.preferences;

import java.io.IOException;

import org.codehaus.groovy.eclipse.GroovyPlugin;
import org.codehaus.groovy.eclipse.core.GroovyCoreActivator;
import org.codehaus.groovy.eclipse.core.builder.GroovyClasspathContainerInitializer;
import org.codehaus.groovy.eclipse.core.compiler.CompilerCheckerParticipant;
import org.codehaus.groovy.eclipse.core.compiler.CompilerUtils;
import org.codehaus.groovy.eclipse.core.preferences.PreferenceConstants;
import org.codehaus.groovy.frameworkadapter.util.SpecifiedVersion;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.groovy.core.Activator;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;

public class CompilerPreferencesPage extends PropertyAndPreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {

    public static final String PREFERENCES_ID = "org.codehaus.groovy.eclipse.preferences.compiler";

    public static final String PROPERTY_ID = Activator.USING_PROJECT_PROPERTIES;

    private IEclipsePreferences preferences;

    private SpecifiedVersion compilerSelection;
    private ComboViewer compilerSelector;
    private Button compilerMismatchCheck;

    private FileFieldEditor configScriptSelector;

    private ScriptFolderSelectorPreferences scriptFolderSelector;

    private Button groovyLibCheck;

    //--------------------------------------------------------------------------

    @Override
    protected Label createDescriptionLabel(Composite parent) {
        // create the project compiler drop-down above the usual "Enable project specific settings" checkbox
        if (isProjectPreferencePage()) {
            createProjectCompilerSection(parent);
        }
        return super.createDescriptionLabel(parent);
    }

    @Override
    protected Control createPreferenceContent(Composite parent) {
        getPreferenceStore(); // ensure preferences are loaded for both modes

        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 1;

        Composite panel = new Composite(parent, SWT.NONE);
        panel.setFont(parent.getFont());
        panel.setLayout(layout);

        if (!isProjectPreferencePage()) {
            createWorkspaceCompilerSection(panel);
        }

        scriptFolderSelector = new ScriptFolderSelectorPreferences(panel, preferences, getPreferenceStore(), getProject());
        scriptFolderSelector.createListContents();

        if (!isProjectPreferencePage()) {
            createClasspathContainerSection(panel);
        }

        return panel;
    }

    private void createProjectCompilerSection(Composite parent) {
        GridLayout layout = new GridLayout();
        layout.horizontalSpacing = 8;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 2;

        Composite panel = new Composite(parent, SWT.NONE);
        panel.setFont(parent.getFont());
        panel.setLayout(layout);
        panel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        // row 1, col 1
        Label compilerLabel = new Label(panel, SWT.WRAP);
        compilerLabel.setFont(parent.getFont());
        compilerLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        compilerLabel.setText("Groovy compiler level for this project:");

        // row 1, col 2
        compilerSelector = new ComboViewer(new Combo(panel, SWT.DROP_DOWN | SWT.READ_ONLY));
        compilerSelector.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof SpecifiedVersion) {
                    return ((SpecifiedVersion) element).toReadableVersionString();
                }
                return "";
            }
        });
        compilerSelector.add(SpecifiedVersion.DONT_CARE);
        for (SpecifiedVersion version : CompilerUtils.getAllGroovyVersions()) {
            compilerSelector.add(version);
        }
        compilerSelection = CompilerUtils.getCompilerLevel(getProject());
        compilerSelector.setSelection(new StructuredSelection(compilerSelection), true);

        // row 2, col *
        GridData gridData = new GridData();
        gridData.horizontalSpan = 2;
        gridData.horizontalIndent = 15;
        gridData.grabExcessHorizontalSpace = false;
        Label explainLabel = new Label(panel, SWT.WRAP);
        explainLabel.setLayoutData(gridData);
        explainLabel.setText("If the project compiler level does not match the workspace compiler level, a build error will appear on the project.");

        // row 3, col *
        gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
        gridData.horizontalSpan = 2;
        gridData.verticalIndent = 15;
        Composite subpanel = new Composite(panel, SWT.NONE);
        subpanel.setFont(parent.getFont());
        subpanel.setLayoutData(gridData);
        configScriptSelector = new FileFieldEditor(CompilerOptions.OPTIONG_GroovyCompilerConfigScript, "Compiler config script:", subpanel) {
            @Override
            protected String changePressed() {
                String value = super.changePressed();
                if (value != null) {
                    IPath path = new Path(value);
                    if (getProject().getLocation().isPrefixOf(path)) {
                        value = path.makeRelativeTo(getProject().getLocation()).toOSString();
                    }
                }
                return value;
            }

            @Override
            public void store() {
                super.store();

                IPreferenceStore store = getPreferenceStore();
                if (store.needsSaving() && store instanceof IPersistentPreferenceStore) {
                    try {
                        ((IPersistentPreferenceStore) store).save();
                    } catch (IOException e) {
                        String message = JFaceResources.format("PreferenceDialog.saveErrorMessage", new Object[] {"Groovy Compiler", e.getMessage()});
                        Policy.getStatusHandler().show(new Status(IStatus.ERROR, Policy.JFACE, message, e), JFaceResources.getString("PreferenceDialog.saveErrorTitle"));
                    }
                }
            };
        };
        configScriptSelector.setFilterPath(getProject().getLocation().toFile());
        configScriptSelector.setPreferenceStore(// this preference is from JavaCore scope
            new ScopedPreferenceStore(new ProjectScope(getProject()), JavaCore.PLUGIN_ID));
        configScriptSelector.load();
    }

    private void createWorkspaceCompilerSection(Composite parent) {
        Label compilerLabel = new Label(parent, SWT.WRAP);
        compilerLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        compilerLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
        compilerLabel.setText("Groovy Compiler settings:");

        Composite compilerPage = new Composite(parent, SWT.NONE | SWT.BORDER);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 3;
        layout.marginWidth = 3;
        layout.numColumns = 1;
        compilerPage.setLayout(layout);
        compilerPage.setFont(parent.getFont());
        compilerPage.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        CompilerSwitchUIHelper.createCompilerSwitchBlock(compilerPage);

        compilerMismatchCheck = new Button(compilerPage, SWT.CHECK);
        compilerMismatchCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        compilerMismatchCheck.setSelection(getCompilerCheckPref());
        compilerMismatchCheck.setText("Enable checking for mismatches between the project and workspace Groovy compiler levels");
    }

    private void createClasspathContainerSection(Composite parent) {
        Label gccLabel = new Label(parent, SWT.WRAP);
        gccLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        gccLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
        gccLabel.setText("Groovy Classpath Container:");

        Composite subsection = new Composite(parent, SWT.BORDER);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 3;
        layout.marginWidth = 3;
        layout.numColumns = 1;
        subsection.setLayout(layout);
        subsection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        groovyLibCheck = new Button(subsection, SWT.CHECK);
        groovyLibCheck.setSelection(getGroovyLibsPref());
        groovyLibCheck.setText("Include all jars in ~/.groovy/lib on the classpath");

        GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
        layoutData.widthHint = 400;

        Label groovyLibLabel = new Label(subsection, SWT.WRAP);
        groovyLibLabel.setLayoutData(layoutData);
        groovyLibLabel.setText("This is the default setting and individual projects can be configured by clicking on the properties page of the Groovy Support classpath container.");

        @SuppressWarnings("unused")
        Label spacerLabel = new Label(subsection, SWT.NONE);

        Button updateButton = new Button(subsection, SWT.PUSH);
        updateButton.setText("Update all Groovy Classpath Containers");
        updateButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateClasspathContainers();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                updateClasspathContainers();
            }
        });

        Label updateLabel = new Label(subsection, SWT.WRAP);
        updateLabel.setLayoutData(layoutData);
        updateLabel.setText("Perform this action if there are changes to ~/.groovy/lib that should be reflected in your projects' classpaths.");
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        IScopeContext scope = (isProjectPreferencePage() ? new ProjectScope(getProject()) : InstanceScope.INSTANCE);
        preferences = scope.getNode(Activator.PLUGIN_ID);
        IPreferenceStore store = new ScopedPreferenceStore(scope, Activator.PLUGIN_ID);
        store.setDefault(Activator.GROOVY_SCRIPT_FILTERS_ENABLED, Activator.DEFAULT_SCRIPT_FILTERS_ENABLED);
        return store;
    }

    @Override
    protected void enablePreferenceContent(boolean enable) {
        super.enablePreferenceContent(enable);
        if (enable) scriptFolderSelector.enableControls();
    }

    @Override
    protected void performDefaults() {
        if (compilerSelector != null) {
            compilerSelector.setSelection(new StructuredSelection(SpecifiedVersion.DONT_CARE), true);
        }

        if (compilerMismatchCheck != null) {
            compilerMismatchCheck.setSelection(true);
        }

        if (configScriptSelector != null) {
            configScriptSelector.setStringValue(null);
        }

        if (groovyLibCheck != null) {
            groovyLibCheck.setSelection(true);
        }

        scriptFolderSelector.restoreDefaults();

        super.performDefaults(); // allow controls to update before project-specific settings callbacks

        if (!isProjectPreferencePage())
            enablePreferenceContent(true);
    }

    @Override
    public boolean performOk() {
        if (compilerSelector != null) {
            StructuredSelection selection = (StructuredSelection) compilerSelector.getSelection();
            SpecifiedVersion selected = (SpecifiedVersion) selection.getFirstElement();
            if (selected == null) {
                selected = SpecifiedVersion.UNSPECIFIED;
            }
            if (selected != compilerSelection && selected != SpecifiedVersion.UNSPECIFIED) {
                CompilerUtils.setCompilerLevel(getProject(), selected, true);
            }
        }

        if (compilerMismatchCheck != null) {
            boolean isSelected = compilerMismatchCheck.getSelection();
            boolean currentPref = getCompilerCheckPref();
            if (!isSelected && currentPref) {
                // delete all markers in the workspace
                try {
                    ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(CompilerCheckerParticipant.COMPILER_MISMATCH_PROBLEM, true, IResource.DEPTH_ONE);
                } catch (CoreException e) {
                    GroovyPlugin.getDefault().logError("Error deleting markers", e);
                }
            }
            if (isSelected != currentPref) {
                setCompilerCheckPref(isSelected);
            }
        }

        if (!isProjectPreferencePage()) {
            setGroovyLibsPref(groovyLibCheck.getSelection());
        } else {
            configScriptSelector.store();
            getPreferenceStore().setValue(PROPERTY_ID, useProjectSettings());
        }

        scriptFolderSelector.applyPreferences();

        return super.performOk();
    }

    @Override
    protected boolean hasProjectSpecificOptions(IProject project) {
        if (project != null && project.equals(getProject())) {
            return getPreferenceStore().getBoolean(PROPERTY_ID);
        }
        return false;
    }

    @Override
    protected String getPreferencePageID() {
        return PREFERENCES_ID;
    }

    @Override
    protected String getPropertyPageID() {
        return PROPERTY_ID;
    }

    //--------------------------------------------------------------------------

    private boolean getCompilerCheckPref() {
        return preferences.getBoolean(Activator.GROOVY_CHECK_FOR_COMPILER_MISMATCH, true);
    }

    private void setCompilerCheckPref(boolean value) {
        preferences.putBoolean(Activator.GROOVY_CHECK_FOR_COMPILER_MISMATCH, value);
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            GroovyPlugin.getDefault().logError("Error saving compiler preferences", e);
        }
    }

    private boolean getGroovyLibsPref() {
        IEclipsePreferences corePrefs = GroovyCoreActivator.getDefault().getPreferences();
        return corePrefs.getBoolean(PreferenceConstants.GROOVY_CLASSPATH_USE_GROOVY_LIB_GLOBAL, true);
    }

    private void setGroovyLibsPref(boolean value) {
        IEclipsePreferences corePrefs = GroovyCoreActivator.getDefault().getPreferences();
        corePrefs.putBoolean(PreferenceConstants.GROOVY_CLASSPATH_USE_GROOVY_LIB_GLOBAL, value);
        try {
            corePrefs.flush();
        } catch (BackingStoreException e) {
            GroovyPlugin.getDefault().logError("Error saving groovy libs preference", e);
        }
    }

    private void updateClasspathContainers() {
        try {
            GroovyClasspathContainerInitializer.updateAllGroovyClasspathContainers();
        } catch (JavaModelException e) {
            GroovyPlugin.getDefault().logError("Problem updating Groovy classpath contianers", e);
        }
    }
}
