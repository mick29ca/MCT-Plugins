/*******************************************************************************
 * Mission Control Technologies, Copyright (c) 2009-2012, United States Government
 * as represented by the Administrator of the National Aeronautics and Space 
 * Administration. All rights reserved.
 *
 * The MCT platform is licensed under the Apache License, Version 2.0 (the 
 * "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations under 
 * the License.
 *
 * MCT includes source code licensed under additional open source licenses. See 
 * the MCT Open Source Licenses file included with this distribution or the About 
 * MCT Licenses dialog available at runtime from the MCT Help menu for additional 
 * information. 
 *******************************************************************************/
package gov.nasa.arc.mct.scenario.component;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.scenario.util.DurationFormatter;
import gov.nasa.arc.mct.services.component.ComponentRegistry;
import gov.nasa.arc.mct.services.component.ComponentTypeInfo;
import gov.nasa.arc.mct.services.component.CreateWizardUI;
import gov.nasa.arc.mct.util.DataValidation;
import gov.nasa.arc.mct.util.MCTIcons;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The Creation dialog box for Activity components.
 * 
 * Contains fields to specify activity start time and duration.
 * 
 */
public class ActivityCreationWizardUI  extends CreateWizardUI {

	private static final ResourceBundle bundle ;
	
	private static final int ICON_HEIGHT = 16;
	private static final int ICON_WIDTH = 16;
	private static final int COL_SIZE = 30;
	private static final int MIN_LENGTH, MAX_LENGTH;

	private static final String ERRORMSG;
	     	
	static {
		bundle = ResourceBundle.getBundle("Bundle"); //NOI18N
		MIN_LENGTH = Integer.parseInt(bundle.getString("MIN_LENGTH")); //NOI18N
		MAX_LENGTH = Integer.parseInt(bundle.getString("MAX_LENGTH")); //NOI18N
		ERRORMSG = String.format(bundle.getString("ERRMSG_LENGTH"), MIN_LENGTH, MAX_LENGTH); //NOI18N
	}
	
	private final JLabel message = new JLabel();
    private final JTextField name = new JTextField();
    private final JTextField startTime = new JTextField("00:00");
    private final JTextField duration = new JTextField("01:00");
    private Class<? extends AbstractComponent> componentClass;
    private Map<ComponentTypeInfo, List<AbstractComponent>> repositories;
    
    // Children which the activity should have upon creation (tags, types)
    private Map<ComponentTypeInfo, List<AbstractComponent>> children = 
    		new HashMap<ComponentTypeInfo, List<AbstractComponent>>();
    
	public ActivityCreationWizardUI(Map<ComponentTypeInfo, List<AbstractComponent>> repositories) {
		this.componentClass = ActivityComponent.class;
		this.repositories = repositories;
		for (ComponentTypeInfo type : repositories.keySet()) {
			children.put(type, new ArrayList<AbstractComponent>());
		}
	}
	
	@Override
	public AbstractComponent createComp(ComponentRegistry comp,
			AbstractComponent targetComponent) {
		String displayName = name.getText().trim();
        AbstractComponent component = null;
        
        component = comp.newInstance(componentClass, targetComponent);
		component.setDisplayName(displayName);
		ActivityComponent activityComponent = (ActivityComponent) component;
		ActivityData data = activityComponent.getData();
		
		long startDate, endDate;
		try {
			startDate = DurationFormatter.parse(startTime.getText());
			endDate = startDate + DurationFormatter.parse(duration.getText()); 
		} catch (ParseException e) {
			startDate = 0L;
			endDate = 30L * 60L * 1000L;
		}
		data.setValue("startTime", String.valueOf(startDate));
		data.setValue("endTime", String.valueOf(endDate));
		
		for (List<AbstractComponent> comps : children.values()) {
			component.addDelegateComponents(comps);
		}
		
		component.save();
		
        return component;
	}
	
	private boolean checkValidity() {
		try {
			return DurationFormatter.parse(startTime.getText()) >= 0 && 
				DurationFormatter.parse(duration.getText()) > 0;
		} catch (ParseException e) {
			return false;
		}
	}

	@Override
	public JComponent getUI(final JButton create) {
		JLabel prompt = new JLabel(bundle.getString("TEXT_FIELD_LABEL")); //NOI18N
		name.setText(bundle.getString("DEFAULT_OBJECT_NAME")); //NOI18N
		prompt.setLabelFor(name);
		name.selectAll();
		name.setColumns(COL_SIZE);
		name.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void changedUpdate(DocumentEvent e) {
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				doAction();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				doAction();
			}

			private boolean verify(String input) {
				return DataValidation.validateLength(input, MIN_LENGTH, MAX_LENGTH);
			}

			private void doAction() {
				
				boolean flag = verify(name.getText().trim());
				create.setEnabled(flag);
				message.setIcon((flag) ? null : MCTIcons.getErrorIcon(ICON_WIDTH, ICON_HEIGHT));
				message.setText((flag) ? "" : ERRORMSG);

			}

		});
	        
		name.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				name.setForeground(Color.BLACK);
			}
		});
	        
		
		JPanel messagePanel = new JPanel();
		messagePanel.add(Box.createRigidArea(new Dimension(2, 17)));
		messagePanel.add(message);
		
		JPanel UIPanel = new JPanel();
		UIPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(10,10,0,0);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 1.0;
		UIPanel.add (prompt, c);
		
		c.gridx = 1;
		c.weightx = 1.0;
		c.insets = new Insets (10,10,4,10);
		UIPanel.add (name,c);
		
		c.insets = new Insets(0,10,4,10);

		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 1;
		c.gridwidth = 1;
		UIPanel.add(new JLabel("Start:"),c);
		
		c.gridx = 1;
		c.gridy = 2;
		c.weightx = 1;
		c.gridwidth = 1;
		UIPanel.add(startTime,c);
		
		c.gridx = 0;
		c.gridy = 3;
		c.weightx = 1;
		c.gridwidth = 1;
		UIPanel.add(new JLabel("Duration:"),c);
		
		c.gridx = 1;
		c.gridy = 3;
		c.weightx = 1;
		c.gridwidth = 1;
		UIPanel.add(duration,c);
		
		for (Entry<ComponentTypeInfo, List<AbstractComponent>> repo : repositories.entrySet()) {
			c.gridy = c.gridy + 1;
			
			c.gridx = 0;
			c.weightx = 1;
			c.gridwidth = 1;
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.WEST;
			// TODO: Safer plural?
			UIPanel.add(new JLabel(repo.getKey().getDisplayName() + "(s):"),c);
			
			c.gridx = 1;
			c.weightx = 1;
			c.gridwidth = 1;
			UIPanel.add(createTypeSelectionButton(repo.getKey()),c);
		}
		
		c.gridx = 0;
		c.gridy = c.gridy + 1;
		c.weightx = 1;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		UIPanel.add(messagePanel,c);
		
		// Enable/disable Create button for valid user input
		DocumentListener buttonEnabler = new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				create.setEnabled(checkValidity());
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				create.setEnabled(checkValidity());				
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				create.setEnabled(checkValidity());			}			
		};
		startTime.getDocument().addDocumentListener(buttonEnabler);
		duration.getDocument().addDocumentListener(buttonEnabler);
				
		UIPanel.setVisible(true);
		return UIPanel;
	}	

	private JButton createTypeSelectionButton(final ComponentTypeInfo type) {
		final JButton button = new JButton("+");
		
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				Window window = SwingUtilities.getWindowAncestor(button);
				final TagSelectionDialog dialog = new TagSelectionDialog(
						repositories.get(type), children.get(type), window);
				dialog.addWindowListener(new WindowAdapter() {

					@Override
					public void windowClosed(WindowEvent evt) {
						Collection<AbstractComponent> result = dialog.getResult();
						if (result != null) {
							children.get(type).clear();
							children.get(type).addAll(result);
						}
					}
					
				});
				dialog.setVisible(true);				
			}			
		});
		
		return button;
	}
}
