package net.logicaltrust.tab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import burp.IBurpExtenderCallbacks;
import burp.ITab;
import net.logicaltrust.SimpleLogger;
import net.logicaltrust.editor.ResponseTextEditor;
import net.logicaltrust.model.MockEntry;
import net.logicaltrust.persistent.MockAdder;
import net.logicaltrust.persistent.MockRepository;
import net.logicaltrust.persistent.SettingsSaver;

public class MockTabPanel extends JPanel implements ITab, MockAdder, HierarchyListener {

	private static final long serialVersionUID = 1L;

	private SimpleLogger logger;
	private MockRepository mockHolder;
	private MockTable mockTable;
	private SettingsSaver settingSaver;
	JTabbedPane tabbedPane;
	ChangeListener changeListener;

	public MockTabPanel(SimpleLogger logger, IBurpExtenderCallbacks callbacks, MockRepository mockHolder, ResponseTextEditor responseEditor, SettingsSaver settingSaver) {
		this.logger = logger;
		this.mockHolder = mockHolder;
		this.settingSaver = settingSaver;
		prepareGui(responseEditor);
		addHierarchyListener(this);
	}
	
	private void prepareGui(ResponseTextEditor responseEditor) {
		setLayout(new BorderLayout(0, 0));
		prepareGitHubFooter();
		prepareCheckBoxTopPanel();
		prepareMain(responseEditor);
	}

	private void prepareMain(ResponseTextEditor responseEditor) {
		mockTable = new MockTable("Mock rules", "rules", mockHolder, null, logger, responseEditor);
		JSplitPane mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mockTable, responseEditor.getComponent());
		add(mainPanel, BorderLayout.CENTER);
		mainPanel.setResizeWeight(0.3f);
	}

	private void prepareGitHubFooter() {
		JPanel githubPanel = new JPanel();
		githubPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(githubPanel, BorderLayout.SOUTH);
		githubPanel.setLayout(new BorderLayout(0, 0));
		
		JLabel githubLabel = createLabelURL("https://github.com/LogicalTrust/BurpHttpMock");
		githubPanel.add(githubLabel);
	}

	private void prepareCheckBoxTopPanel() {
		JPanel checkboxPanel = new JPanel();
		add(checkboxPanel, BorderLayout.NORTH);
		checkboxPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		
		JCheckBox chckbxDebug = new JCheckBox("Debug output");
		chckbxDebug.setSelected(settingSaver.isDebugOn());
		chckbxDebug.addActionListener(e -> { settingSaver.saveDebugOutput(chckbxDebug.isSelected()); });
		checkboxPanel.add(chckbxDebug);
		
		JButton changePort = new JButton("Advanced");
		changePort.addActionListener(e -> handleChangePortButton());
		checkboxPanel.add(changePort);
	}
	
	private void handleChangePortButton() {	
		int initValue = settingSaver.loadPort();
		String input = (String)JOptionPane.showInputDialog(this, "Set port number for local server",
                "Advanced settings", JOptionPane.QUESTION_MESSAGE, null, null, initValue + "");
		
		if (input == null)
			return;
		
		try {
			int port = Integer.parseInt(input);
			if (port > 0 && port < 65536) {
				if (port != initValue) {
					settingSaver.savePort(port);
					JOptionPane.showMessageDialog(this, "The change will take effect after restart", "Success", JOptionPane.INFORMATION_MESSAGE);
				}
				return;
			}
		} catch (NumberFormatException e1) { 
			logger.debug("Cannot parse " + input);
		}
		JOptionPane.showMessageDialog(this, "Invalid value. Port must be between 1 and 65535", "Invalid value", JOptionPane.ERROR_MESSAGE);
	}

	private JLabel createLabelURL(String url) {
		JLabel lblUrl = new JLabel(url);
		lblUrl.setForeground(Color.BLUE);
		lblUrl.setCursor(new Cursor(Cursor.HAND_CURSOR));
		lblUrl.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().browse(new URI(lblUrl.getText()));
				} catch (URISyntaxException | IOException ex) {
					ex.printStackTrace(logger.getStderr());
				}
			}
		});
		return lblUrl;
	}

	@Override
	public String getTabCaption() {
		return "HTTP Mock";
	}

	@Override
	public Component getUiComponent() {
		return this;
	}

	@Override
	public void addMock(MockEntry entry) {
		mockTable.addMock(entry);
		highlightTab();
	}

	void highlightTab()
	{
		if(tabbedPane != null)
		{
			for(int i = 0; i < tabbedPane.getTabCount(); i++)
			{
				if(tabbedPane.getComponentAt(i) == this)
				{
					tabbedPane.setBackgroundAt(i, new Color(0xff6633));
					Timer timer = new Timer(3000, new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							for(int j = 0; j < tabbedPane.getTabCount(); j++)
							{
								if (tabbedPane.getComponentAt(j) == MockTabPanel.this)
								{
									tabbedPane.setBackgroundAt(j, Color.BLACK);
									break;
								}
							}
						}
					});
					timer.setRepeats(false);
					timer.start();
					break;
				}
			}
		}
	}

	@Override
	public void hierarchyChanged(HierarchyEvent e)
	{
		tabbedPane = (JTabbedPane) getParent();
		changeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if(tabbedPane.getSelectedComponent() == MockTabPanel.this)
				{
					tabbedPane.setBackgroundAt(tabbedPane.getSelectedIndex(), Color.BLACK);
				}
			}
		};
		tabbedPane.addChangeListener(changeListener);
		removeHierarchyListener(this);
	}

	// call from extensionUnloaded
	void removeChangeListener()
	{
		if (changeListener != null)
		{
			tabbedPane.removeChangeListener(changeListener);
		}
	}
}
