package org.yamcs.simulator.ui;

import java.awt.*;

import javax.swing.JFrame;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JPanel;
import javax.swing.JButton;

import org.jgroups.stack.RouterStub;
import org.yamcs.simulator.ServerConnection;
import org.yamcs.simulator.Simulator;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.LinkedList;

public class SimWindow {

	private JFrame frame;
	private Simulator simClient;


	public void setSimClient(Simulator simClient) {
		this.simClient = simClient;
	}

	/**
	 * Create the application.
	 */
	public SimWindow(Simulator client) {
		initialize(client.getNbServerNodes());

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    getFrame().setVisible(true);
                    setSimClient(client);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        });
	}


    private class ServerNodeUi
    {
        JTextArea sWindow;
        JTextField sStatus;
        JScrollPane sScroll;
    }
    LinkedList<ServerNodeUi> serverNodeUis = new LinkedList<>();

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize(int nbServerNodes) {
        setFrame(new JFrame());
        frame.setTitle("YAS - Yet Another Simulator");
		getFrame().setBounds(100, 100, 795, 474);
		getFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        for(int i = 0; i < nbServerNodes; i++)
        {
            ServerNodeUi serverNodeUi = new ServerNodeUi();
            serverNodeUi.sWindow = new JTextArea();
            serverNodeUi.sStatus = new JTextField();
            serverNodeUi.sStatus.setHorizontalAlignment(SwingConstants.CENTER);
            serverNodeUi.sStatus.setText("Server " + (i+1) + " status");
            serverNodeUi.sStatus.setColumns(10);

            serverNodeUi.sScroll = new JScrollPane();
            serverNodeUi.sScroll.setViewportView(serverNodeUi.sWindow);

            serverNodeUis.add(serverNodeUi);
        }

		JPanel panel = new JPanel();
		GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
        GroupLayout.SequentialGroup sequentialGroup1 = groupLayout.createSequentialGroup();
        GroupLayout.SequentialGroup sequentialGroup2 = groupLayout.createSequentialGroup();
        GroupLayout.ParallelGroup parallelGroup1 = groupLayout.createParallelGroup(Alignment.LEADING);
        GroupLayout.ParallelGroup parallelGroup2 = groupLayout.createParallelGroup(Alignment.LEADING);
        for(ServerNodeUi s : serverNodeUis)
        {
            sequentialGroup1.addComponent(s.sStatus, GroupLayout.PREFERRED_SIZE, 269, GroupLayout.PREFERRED_SIZE).addGap(30);
            sequentialGroup2.addComponent(s.sScroll, GroupLayout.PREFERRED_SIZE, 269, GroupLayout.PREFERRED_SIZE).addGap(32);
            parallelGroup1.addComponent(s.sStatus, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
            parallelGroup2.addComponent(s.sScroll, GroupLayout.PREFERRED_SIZE, 377, GroupLayout.PREFERRED_SIZE);
        }

		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(panel, GroupLayout.PREFERRED_SIZE, 172, GroupLayout.PREFERRED_SIZE)
					.addGap(129)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                            .addGroup(sequentialGroup1)
                            .addGroup(sequentialGroup2)))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(12)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(panel, GroupLayout.PREFERRED_SIZE, 403, GroupLayout.PREFERRED_SIZE)
						.addGroup(groupLayout.createSequentialGroup()
							.addGroup(parallelGroup1)
							.addGap(12)
							.addGroup(parallelGroup2)))
					.addContainerGap(26, Short.MAX_VALUE))
		);
		

		JButton losBtn = new JButton("Start LOS/AOS");
		losBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
                if(losBtn.getText().contains("Start")) {
                    simClient.startTriggeringLos();
                    losBtn.setText("Set AOS");
                }
                else
                {
                    simClient.stopTriggeringLos();
                    losBtn.setText("Start LOS/AOS");
                }
			}
		});
		losBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		
		JButton tLosButton = new JButton("Dump LOS Data");
		tLosButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {

                simClient.dumpLosDataFile(null);
            }
        });
		tLosButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
            }
        });
        panel.add(losBtn);
		panel.add(tLosButton);
		frame.getContentPane().setLayout(groupLayout);
	}

	public JFrame getFrame() {
		return frame;
	}

	public void setFrame(JFrame frame) {
		this.frame = frame;
	}


    public void setServerStatus(int id, ServerConnection.ConnectionStatus connectionStatus)
    {
        JTextField sStatus = serverNodeUis.get(id).sStatus;

        if(connectionStatus == ServerConnection.ConnectionStatus.CONNECTING) {
            sStatus.setText("Listening for Yamcs");
            sStatus.setBackground(Color.RED);
        }

        if(connectionStatus == ServerConnection.ConnectionStatus.CONNECTED) {
            sStatus.setText("Connected to Yamcs");
            sStatus.setBackground(Color.GREEN);
        }
    }

    public void addLog(int id, String s) {
        JTextArea sWindow = serverNodeUis.get(id).sWindow;
        sWindow.append(s);
    }

}
