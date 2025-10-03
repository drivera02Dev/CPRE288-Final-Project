package v2;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.Dataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;


///Okay everything works except the border and i think the scanned objects 
///are a little broken
@SuppressWarnings("serial")
public class UI extends JFrame {
    private XYSeries series;
    private ArrayList<XYSeries> seriesHistory;
    private int selectedSeries;
    private XYSeriesCollection dataset;
    private JButton viewLeft;
    private JButton viewRight;
    private Socket socket;
    private BufferedReader reader;
    private OutputStream writer;
    private JTextField commandField;
    private JFreeChart chart;
    private JLabel statusUpdate;
    private ArrayList<ScannedObject> objects;
    private MapPanel mapPanel;

    public UI() {
    	
    	//Map to show cybot movement and 
    	mapPanel = new MapPanel();
    	JFrame mapFrame = new JFrame("Map");
    	mapFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
    	mapFrame.add(mapPanel);
    	mapFrame.pack();
    	mapFrame.setVisible(true);
    	
    	objects = new ArrayList<ScannedObject>(5);

        setTitle("CyBot UI");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        series = new XYSeries("Data");
        dataset = new XYSeriesCollection(series);
        chart = ChartFactory.createXYLineChart(
                "IR Sensor", "angle", "raw", dataset);

        //make series history for easy toggling between previous scans
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
        add(viewLeft, BorderLayout.LINE_START); 	//left and right history buttons
        add(viewRight, BorderLayout.LINE_END);

        ChartPanel chartPanel = new ChartPanel(chart);
        add(chartPanel, BorderLayout.CENTER);


        commandField = new JTextField();
        commandField.addActionListener(e -> {
			try {
				sendCommand();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
        add(commandField, BorderLayout.SOUTH);

        this.statusUpdate = new JLabel("cyBot waiting");
        this.statusUpdate.setSize(300, 40);
        this.statusUpdate.setAlignmentX(CENTER_ALIGNMENT);
        this.statusUpdate.setHorizontalTextPosition(SwingConstants.CENTER);	//this shit dont work but whatever
        add(statusUpdate, BorderLayout.NORTH);

        setVisible(true);
        connectAndListen();	//Listens to messages from cybot on another thread
    }
    


    @SuppressWarnings("unchecked")
	private void parseObjectData(String s) {
    	ArrayList<Integer> splits = new ArrayList<Integer>(20);
    	
    	for(int i = 0; i < s.length(); i++) {
    		if(s.charAt(i) == ',') {
    			splits.add(i);
    		}
    	}
    	if(splits.size() != 3) {
    		System.out.println("bad object usage");
    	}

    	int startAngle = Integer.valueOf(s.substring(0,splits.get(0)));
    	int endAngle = Integer.valueOf(s.substring(splits.get(0)+1, splits.get(1)));
    	double distance = Double.valueOf(s.substring(splits.get(1)+1, splits.get(2)));
    	double width = Double.valueOf(s.substring(splits.get(2) + 1, s.length()));

    	ScannedObject newObject = new ScannedObject(startAngle, endAngle, distance, width);
    	objects.add(newObject);
    	
    	//objects.removeAll(objects);

    }
    
    private void handleObjects(BufferedReader reader) {
    	//start angle, end angle, distance, width
    	String s = "";
    	try {
			while((s = reader.readLine()) != null) {
				if(s.equals("end objects")) {
					for(ScannedObject o : objects) {
						mapPanel.addObjects((ArrayList<ScannedObject>) objects.clone());	
						System.out.println("\n" + o.startAngle + " " + o.endAngle + " " + o.distance + " " + o.width);
					}
					return;
				}
				else {
					parseObjectData(s);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    
    
    
    
    private void connectAndListen() {
        new Thread(() -> {
            try {
                System.out.println("Connecting to 192.168.1.1:288...");
                socket = new Socket("192.168.1.1", 288);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = socket.getOutputStream();
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

                    else if(s.contains("moved")) {
                    	split = s.indexOf(',');
                    	mapPanel.cyBot_move(Integer.valueOf(s.substring(split+1, s.length())));
                    }
                    else if(s.contains("error")) {
                    	split = s.indexOf(',');
                    	handleCyBotError(s.substring(split+1, s.length()));
                    }
                    else if(s.contains("turned")) {
                    	split = s.indexOf(',');
                    	mapPanel.cyBot_turn(Integer.valueOf(s.substring(split+1, s.length())));
                    }
                    else if (s.equals("done")) {
                    	this.statusUpdate.setText("cyBot waiting...");
                    }

                    else if(s.equals("start objects")) {
                    	handleObjects(reader); 
                    }

                }
                System.out.println("\nConnection closed.");
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleCyBotError(String error) {
		if(error.contains("bump")) {
			int split = error.indexOf(',');
			if(error.substring(split+1).contains("eft")) {
				mapPanel.cyBot_hitObject(true);	//boolean is if hit left or right
			}
			else {
				mapPanel.cyBot_hitObject(false);
			}
		}
		else if(error.contains("boundary")) {
			mapPanel.cyBot_hitBoundary();
		}
	}



    //THIS ONLY WORKS BETWEEN [0,255]
	//this sends command in the form [char opcode][char param1][char param2]. Inputs are integers (except opcode
	// and are converted to chars before sent (i.e w,100,0)
    private void sendCommand() throws InterruptedException, IOException {
        if (writer != null) {
        	if(statusUpdate.getText().equals("cyBot busy")) {
        		System.out.println("cybot busy, try again later");
        		return;
        	}
            String command = commandField.getText();
            int splits[] = new int[2];
            splits[0] = command.indexOf(',');
            splits[1] = command.substring(splits[0]+1).indexOf(',') + splits[0]+1;
            
            char opcode = command.charAt(splits[0]-1);

            int p = Integer.valueOf(command.substring(splits[0]+1, splits[1]));
            
            char param1 = (char) p;
            
            int p2 = Integer.valueOf(command.substring(splits[1]+1));
            
            char param2 = (char) p2;
            
            writer.write((byte)opcode);
            Thread.sleep(50);
            writer.write((byte) param1);
			Thread.sleep(50);
            writer.write((byte) param2);
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
        SwingUtilities.invokeLater(UI::new);
    }
}

class ScannedObject {
	public int startAngle;
	public int endAngle;
	public double distance;
	public double width;
	
	public ScannedObject(int startAngle, int endAngle, double distance, double width) {
		this.startAngle = startAngle;
		this.endAngle = endAngle;
		this.distance = distance;
		this.width = width;
	}

}