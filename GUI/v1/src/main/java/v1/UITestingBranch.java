package v1;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.Dataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import v1.Vector;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;




///A WARNING FOR ALL DEBUGGING HEADACHES: I THINK ALL INSTANCES OF UI's MUST BE CLOSED BEFORE STARTING ANOTHER
///
///usage:
///run lab 11 as is 
///run ui with bot connected to wifi
///send commands in textbox of ui (hit enter) in the form "opcode, param1 (as integer), param2 (as integer)"
///example: 
///w,100,0  //move forwards 100
///p,0,180  //scan range (uses graph)
///
@SuppressWarnings("serial")
public class UITestingBranch extends JFrame {
    private XYSeries series;
    private ArrayList<XYSeries> seriesHistory;
    private int selectedSeries;
    private XYSeriesCollection dataset;
    private FieldMap fieldMap;
    private JButton viewLeft;
    private JButton viewRight;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private JTextField commandField;
    private JFreeChart chart;
    private JLabel statusUpdate;
    private Vector cyBOT_position;
    
    private class FieldMap {
    	private XYSeries map_series;
		private XYSeriesCollection map_dataset;
		private JFreeChart map_field;
		private ChartPanel mapPanel;
		private JPanel mapContainer;
		

		public FieldMap() {
			map_series = new XYSeries("MapData");
			map_dataset = new XYSeriesCollection(map_series);
			map_field = ChartFactory.createScatterPlot("Map", "x1", "x2", map_dataset);		
			mapContainer = new JPanel();
			mapContainer.setLayout(new BorderLayout());
			mapPanel = new ChartPanel(map_field);
			mapPanel.setLayout(new BorderLayout());
			mapContainer.add(mapPanel, BorderLayout.CENTER);
			map_series.add(500, 500);
			XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
			renderer.setSeriesPaint(selectedSeries, getForeground());
		}
		
	
		
    }
    
    public UITestingBranch() {
    	
    	this.cyBOT_position = new Vector(0,0);
		//JFRAME OPTIONS
		setTitle("CyBot UI");
		setSize(800, 600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		//IR SENSOR CHART AND MAP FIELD 
		series = new XYSeries("Data");
		dataset = new XYSeriesCollection(series);

		chart = ChartFactory.createXYLineChart(
				"IR Sensor", "angle", "raw", dataset);

		ChartPanel chartPanel = new ChartPanel(chart);
		JPanel chartContainer = new JPanel();
		chartContainer.setLayout(new BorderLayout());
		chartContainer.add(chartPanel, BorderLayout.CENTER);
		

		
		//History for easy traversal between previous scans
		seriesHistory = new ArrayList<>(10);
		seriesHistory.add(series);
		viewLeft = new JButton("<");
		viewLeft.addActionListener(e -> {
			try {
				selectedSeries--;
				if(selectedSeries < 0) {
					System.out.println("Already at earliest chart");
					selectedSeries++;
					throw new Exception();
				}
				dataset.removeAllSeries();
				dataset.addSeries(seriesHistory.get(selectedSeries));
			}
			catch(Exception err) {
				
			}
		});
		viewRight = new JButton(">");
		viewRight.addActionListener(e -> {
			try {
				selectedSeries++;
				if(selectedSeries >= seriesHistory.size()) {
					System.out.println("Already at latest chart");
					selectedSeries--;
					throw new Exception();
				}
				dataset.removeAllSeries();
				dataset.addSeries(seriesHistory.get(selectedSeries));
			}
			catch(Exception err) {
				
			}
		});


		commandField = new JTextField();
		commandField.addActionListener(e -> {
			try {
				sendCommand();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		this.statusUpdate = new JLabel("cyBot waiting");
		this.statusUpdate.setSize(300, 40);
		this.statusUpdate.setAlignmentX(CENTER_ALIGNMENT);
		this.statusUpdate.setHorizontalTextPosition(SwingConstants.CENTER);	//this shit dont work but whatever
		

		this.fieldMap = new FieldMap();

		chartContainer.add(viewLeft, BorderLayout.WEST);
		chartContainer.add(viewRight, BorderLayout.EAST);	
		//add(chartPanel, BorderLayout.CENTER);
		add(chartContainer, BorderLayout.CENTER);
		add(fieldMap.mapContainer, BorderLayout.EAST);
		add(commandField, BorderLayout.SOUTH);
		add(statusUpdate, BorderLayout.NORTH);
		setVisible(true);
		connectAndListen();

    }
    
    private void addBorder() {
    	
    }

    private void connectAndListen() {
        new Thread(() -> {
            try {
                System.out.println("Connecting to 192.168.1.1:288...");
                socket = new Socket("192.168.1.1", 288);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                System.out.println("Connected.");

                String s = ""; 
                int split = 0;
                //ALL COMMANDS ARE FOLLOWED BY A NEW LINE NOW 
                while ((s = reader.readLine()) != null) {
                    System.out.println(s); // Just print each character as received
                   
                    if(s.equals("start scan")) {
						dataset.removeAllSeries();
						series = new XYSeries("Data");
						dataset.addSeries(series);
						seriesHistory.add(series);
						selectedSeries = seriesHistory.size()-1;
						XYPlot plt = (XYPlot) chart.getPlot();
						plt.getDomainAxis().setInverted(true);
                    	while(!(s = reader.readLine()).equals("end scan")) {
                    		split = s.indexOf(',');
                    		this.series.add(Integer.valueOf(s.substring(0, split)), Integer.valueOf(s.substring(split + 1, s.length())));
                    	}
                    	this.repaint();				
                    }

                    if(s.equals("forwards")) {
                    	split = s.indexOf(',');
                    	System.out.println(Integer.valueOf(s.substring(split+1, s.length())));
                    }
                    if(s.equals("done")) {
                    	this.statusUpdate.setText("cyBot waiting...");
                    }
                }
                System.out.println("\nConnection closed.");
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    //this sends command in the form [char opcode][char param1][char param2]. Inputs are integers (except opcode
	// and are converted to chars before sent (i.e w,100,0)
    private void sendCommand() throws InterruptedException {
        if (writer != null) {
            String command = commandField.getText();
            int splits[] = new int[2];
            splits[0] = command.indexOf(',');
            splits[1] = command.substring(splits[0]+1).indexOf(',') + splits[0]+1;
            
            char opcode = command.charAt(splits[0]-1);

            int p = Integer.valueOf(command.substring(splits[0]+1, splits[1]));
            char param1 = (char) p;
            
            int p2 = Integer.valueOf(command.substring(splits[1]+1));
            char param2 = (char) p2;
            
            writer.print(opcode);
            Thread.sleep(50);
            writer.print(param1);
			Thread.sleep(50);
            writer.print(param2);
            Thread.sleep(50);
            writer.flush();
            
            System.out.print(opcode + " " + p + " " + p2);
            commandField.setText("");
            this.statusUpdate.setText("cyBot busy");
        }
        else {
        	System.out.println("gate 2");
        }
    }

    ///Entry
    public static void main(String[] args) {
        SwingUtilities.invokeLater(UITestingBranch::new);
    }
}

