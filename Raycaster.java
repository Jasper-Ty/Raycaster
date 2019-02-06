import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

public class Raycaster extends JFrame{
	
	//constants
	public static final int CAMERA_WIDTH = 500; //px
	public static final int MINIMAP_WIDTH = 500; //px
	public static final double FIELD_OF_VIEW = 70; //deg
	public static final int NUM_RAYS = 100;
	public static final int COLUMN_WIDTH = CAMERA_WIDTH/NUM_RAYS; //px 

	public static final int PLAYER_RADIUS = 5;
	public static final int ARROW_LENGTH = 15;
	public static final double TURN_SPEED = 3; //deg
	
	public static final int LEVEL_WIDTH = 25;
	public static final int SQUARE_WIDTH = MINIMAP_WIDTH / LEVEL_WIDTH; //px
	
	Camera game_camera;
	MiniMap game_minimap;
	
	//player position & orientation variables
	double player_x;
	double player_y;
	double view_angle;
	int move_direction; //-1: backwards, 0: stationary, 1: forward
	int turn_direction; //-1: clockwise 0: stationary, 1: counterclockwise
	
	//if the array element at [3][7] is true, that means there's a wall at (3, 7), if it's false, then it's open space
	boolean[][] game_level;
	
	//tells me if a key is pressed
	boolean key_up;
	boolean key_down;
	boolean key_left;
	boolean key_right;
	
	//shoots a ray given the endpoint and the angle, and returns the distance to the first grid edge intersected
	public double castRay(double start_x, double start_y, double ray_direction){
		
		//the coordinates for the intersection w/ the grid edge
		double hit_x;
		double hit_y;
		double distance;
		
		//endpoint distance from grid edges
		int start_col = (int)start_x/SQUARE_WIDTH;
		int start_row = (int)start_y/SQUARE_WIDTH;
		double dxplus = ((start_col + 1) * SQUARE_WIDTH) - start_x;
		double dxminus = (start_col * SQUARE_WIDTH) - start_x;
		double dyplus = ((start_row + 1) * SQUARE_WIDTH) - start_y;
		double dyminus = (start_row * SQUARE_WIDTH) - start_y;
		
		//band-aid fix for certain cases :)
		if(dxminus == 0.0){
			dxminus = -SQUARE_WIDTH;
		}
		if(dxplus == 0.0){
			dxminus = SQUARE_WIDTH;
		}
		if(dyminus == 0.0){
			dyminus = -SQUARE_WIDTH;
		}
		if(dyplus == 0.0){
			dyminus = SQUARE_WIDTH;
		}
		
		//splits up the code for different quadrants and the trivial cases: 90 degree multiples.
		if(ray_direction == 0){
			hit_y = start_y;
			hit_x = start_x + dxplus;
			distance = dxplus;
		}else if(ray_direction < 90 && ray_direction > 0){
			//this could probably be way better but i havent 100% figured it out yet
			double u = dxplus * Math.tan(Math.toRadians(ray_direction));
			double v = dyplus * (1.0 / Math.tan(Math.toRadians(ray_direction)));
			if(u <= dyplus){
				hit_y = start_y + u;
				hit_x = start_x + dxplus; 
				distance = Math.sqrt(dxplus*dxplus + u*u);
			}else{
				hit_x = start_x + v;
				hit_y = start_y + dyplus;
				distance = Math.sqrt(dyplus*dyplus + v*v);
			}
		}else if(ray_direction == 90){
			hit_x = start_x;
			hit_y = start_y + dyplus;
			distance = dyplus;
		}else if(ray_direction > 90 && ray_direction < 180){
			double u = dxminus * Math.tan(Math.toRadians(ray_direction));
			double v = dyplus * (1.0 / Math.tan(Math.toRadians(ray_direction)));
			if(u <= dyplus){
				hit_y = start_y + u;
				hit_x = start_x + dxminus; 
				distance = Math.sqrt(dxminus*dxminus + u*u);
			}else{
				hit_x = start_x + v;
				hit_y = start_y + dyplus;
				distance = Math.sqrt(dyplus*dyplus + v*v);
			}
		}else if(ray_direction == 180){
			hit_y = start_y;
			hit_x = start_x + dxminus;
			distance = dxminus;
		}else if(ray_direction > 180 && ray_direction < 270){
			double u = dxminus * Math.tan(Math.toRadians(ray_direction));
			double v = dyminus * (1.0 / Math.tan(Math.toRadians(ray_direction)));
			if(u >= dyminus){
				hit_y = start_y + u;
				hit_x = start_x + dxminus; 
				distance = Math.sqrt(dxminus*dxminus + u*u);
			}else{
				hit_x = start_x + v;
				hit_y = start_y + dyminus;
				distance = Math.sqrt(dyminus*dyminus + v*v);
			}
		}else if(ray_direction == 270){
			hit_y = start_y + dyminus;
			hit_x = start_x;
			distance = -dyminus;
		}else if(ray_direction > 270 && ray_direction < 360){
			double u = dxplus * Math.tan(Math.toRadians(ray_direction));
			double v = dyminus * (1.0 / Math.tan(Math.toRadians(ray_direction)));
			if(u >= dyminus){
				hit_y = start_y + u;
				hit_x = start_x + dxplus; 
				distance = Math.sqrt(dxplus*dxplus + u*u);
			}else{
				hit_x = start_x + v;
				hit_y = start_y + dyminus;
				distance = Math.sqrt(dyminus*dyminus + v*v);
			}	
		}else{
			return 0;
		}
		
		//checks to see if the edge hit is a wall edge
		int hit_col = 0;
		int hit_row = 0;
		if((hit_x % SQUARE_WIDTH) == 0 && (ray_direction > 270 || ray_direction < 90)){
			hit_col = ((int)hit_x/SQUARE_WIDTH);
			hit_row = ((int)hit_y/SQUARE_WIDTH);
		}else if((hit_y % SQUARE_WIDTH) == 0 && (ray_direction > 0 && ray_direction < 180)){
			hit_col = ((int)hit_x/SQUARE_WIDTH);
			hit_row = ((int)hit_y/SQUARE_WIDTH);
		}else if((hit_x % SQUARE_WIDTH) == 0 && (ray_direction > 90 && ray_direction < 270)){
			hit_col = ((int)hit_x/SQUARE_WIDTH) - 1;
			hit_row = ((int)hit_y/SQUARE_WIDTH);
		}else if((hit_y % SQUARE_WIDTH) == 0 && (ray_direction > 180 && ray_direction < 360)){
			hit_col = ((int)hit_x/SQUARE_WIDTH);
			hit_row = ((int)hit_y/SQUARE_WIDTH) - 1;
		}
		if(game_level[hit_col][hit_row]){
			return distance;
		}else{
			//if not, continue shooting the ray
			return distance + castRay(hit_x, hit_y, ray_direction);
		}
	}
	
	public static void main(String[] args){
		JFrame.setDefaultLookAndFeelDecorated(true);
		Raycaster game = new Raycaster();
		game.start();
	}
	
	public Raycaster(){
		
		//sets up the JFrame
		super("Raycaster");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		
		//loads game level
		game_level = new boolean[LEVEL_WIDTH][LEVEL_WIDTH];
		try{
			FileReader levelFile = new FileReader(new File("level.txt"));
			BufferedReader readLevel = new BufferedReader(levelFile);
			for(int row = 0; row < LEVEL_WIDTH; row++){
				for(int col = 0; col < LEVEL_WIDTH; col++){
					if((char)readLevel.read() == ' '){
						game_level[col][row] = false;
					}else{
						game_level[col][row] = true;
					}
				}
				readLevel.readLine();
			}
		}catch(FileNotFoundException e){
			System.out.print("File Not Found Exception");
		}catch(IOException e){
			System.out.print("IOException");
		}
		
		//creates the MiniMap and Camera
		player_x = 30;
		player_y = 30;
		view_angle = 0;
		game_minimap = new MiniMap(player_x, player_y, view_angle, game_level);
		game_camera = new Camera();
		
		//sets up key input
		key_up = false;
		key_down = false;
		key_left = false;
		key_right = false;
		addKeyListener(new KeyListener(){
			@Override
			public void keyPressed(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_UP){
					key_up = true;
					move_direction = 1;
				}
				if(e.getKeyCode() == KeyEvent.VK_DOWN){
					key_down = true;
					move_direction = -1;
				}
				if(e.getKeyCode() == KeyEvent.VK_LEFT){
					key_left = true;
					turn_direction = 1;
				}
				if(e.getKeyCode() == KeyEvent.VK_RIGHT){
					key_right = true;
					turn_direction = -1;
				}
			}
			@Override
			public void keyReleased(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_UP){
					key_up = false;
					if(key_down == true){
						move_direction = -1;
					}else{
						move_direction = 0;
					}
				}
				if(e.getKeyCode() == KeyEvent.VK_DOWN){
					key_down = false;
					if(key_up == true){
						move_direction = 1;
					}else{
						move_direction = 0;
					}
				}
				if(e.getKeyCode() == KeyEvent.VK_LEFT){
					key_left = false;
					if(key_right == true){
						turn_direction = -1;
					}else{
						turn_direction = 0;
					}
				}
				if(e.getKeyCode() == KeyEvent.VK_RIGHT){
					key_right = false;
					if(key_left == true){
						turn_direction = 1;
					}else{
						turn_direction = 0;
					}
				}
			}
			public void keyTyped(KeyEvent e){
			}
		});
		
		//adds the components and finalizes and shows the JFrame
		setLayout(new GridLayout(1,2));
		add(game_minimap);
		add(game_camera);
		pack();
		setVisible(true);
	}
	
	public void start(){
		//starts the game loop
		Timer timer = new Timer();
		timer.schedule(new TimerTask(){
			public void run(){
				
				//movement code
				if(move_direction == 1 || move_direction == -1){
					
					double dx = Math.cos(Math.toRadians(view_angle));
					double dy = Math.sin(Math.toRadians(view_angle));
					
					double prev_x = player_x;					
					double prev_y = player_y;
					
					double new_x = player_x;
					double new_y = player_y;
					if(move_direction == 1){
						new_x += dx;
						new_y += dy;
					}else if(move_direction == -1){
						new_x -= dx;
						new_y -= dy;
					}
					
					//variable hell
					double new_left_x = new_x - PLAYER_RADIUS;
					double new_right_x = new_x + PLAYER_RADIUS - 1;
					double new_top_y = new_y - PLAYER_RADIUS;
					double new_bottom_y = new_y + PLAYER_RADIUS - 1;
					double prev_left_x = prev_x - PLAYER_RADIUS;
					double prev_right_x = prev_x + PLAYER_RADIUS - 1;
					double prev_top_y = prev_y - PLAYER_RADIUS;
					double prev_bottom_y = prev_y + PLAYER_RADIUS - 1;
					
					int new_left_col = (int)new_left_x / SQUARE_WIDTH;
					int new_right_col = (int)new_right_x / SQUARE_WIDTH;
					int new_top_row = (int)new_top_y / SQUARE_WIDTH;
					int new_bottom_row = (int)new_bottom_y / SQUARE_WIDTH;
					int prev_left_col = (int)prev_left_x / SQUARE_WIDTH;
					int prev_right_col = (int)prev_right_x / SQUARE_WIDTH;
					int prev_top_row = (int)prev_top_y / SQUARE_WIDTH;
					int prev_bottom_row = (int)prev_bottom_y / SQUARE_WIDTH;

					//collision detection; if a wall is hit, set position to just touch the wall
					if((game_level[prev_left_col][new_top_row] || game_level[prev_right_col][new_top_row])
						&& (new_top_row != prev_top_row)){
						new_y = ((new_top_row + 1)* SQUARE_WIDTH) + PLAYER_RADIUS;
					}
					if((game_level[prev_left_col][new_bottom_row] || game_level[prev_right_col][new_bottom_row])
						&& (new_bottom_row != prev_bottom_row)){
						new_y = ((new_bottom_row) * SQUARE_WIDTH) - PLAYER_RADIUS;
					}
					if((game_level[new_left_col][prev_top_row] || game_level[new_left_col][prev_bottom_row])
						&& (!game_level[prev_left_col][prev_top_row] && !game_level[prev_left_col][prev_bottom_row])){
						new_x = ((new_left_col + 1)* SQUARE_WIDTH) + PLAYER_RADIUS;
					}
					if((game_level[new_right_col][prev_top_row] || game_level[new_right_col][prev_bottom_row])
						&& (!game_level[prev_right_col][prev_top_row] && !game_level[prev_right_col][prev_bottom_row])){
						new_x = (new_right_col* SQUARE_WIDTH) - PLAYER_RADIUS;
					}
					player_x = new_x;
					player_y = new_y;	
				}
				if(turn_direction == 1){
					view_angle = (((view_angle - TURN_SPEED) % 360) + 360) % 360;
				}
				if(turn_direction == -1){
					view_angle = (((view_angle + TURN_SPEED) % 360) + 360) % 360;
				}
				
				double ray_angle = (((view_angle - (FIELD_OF_VIEW/2))%360) + 360) % 360;
				for(int i = 0; i < NUM_RAYS; i++){
					int testRay = (int)(4000.0 / castRay(player_x, player_y, ray_angle));
					game_camera.updateColumn(i, Math.abs(testRay));
					ray_angle = (((ray_angle + (FIELD_OF_VIEW/NUM_RAYS))%360) + 360) % 360;
				}
				game_camera.repaint();
				
				game_minimap.updatePosition(player_x, player_y, view_angle);
				game_minimap.repaint();
			}
		}, 0, 1000/60);
	}
	
}
class Camera extends JPanel{
	
	double[] column_data;
	
	public Camera(){
		column_data = new double[Raycaster.NUM_RAYS];
		for(int i = 0; i < column_data.length; i++){
			column_data[i] = Raycaster.CAMERA_WIDTH;
		}
	}
	
	public void updateColumn(int column_num, int height_in){
		if(height_in > Raycaster.CAMERA_WIDTH){
			column_data[column_num] = Raycaster.CAMERA_WIDTH;
		}else{
			column_data[column_num] = height_in;
		}
	}
	
	@Override
	public Dimension getPreferredSize() {
	    return new Dimension(Raycaster.CAMERA_WIDTH, Raycaster.CAMERA_WIDTH);
	}
	
	@Override
	public void paintComponent(Graphics g){
		Graphics2D g2D = (Graphics2D) g;
		super.paintComponent(g2D);
		
		//draws background
		g2D.setColor(new Color(50, 0, 0));
		g2D.fillRect(0, Raycaster.CAMERA_WIDTH/2, Raycaster.CAMERA_WIDTH, Raycaster.CAMERA_WIDTH/2);
		g2D.setColor(new Color(0, 0, 0));
		g2D.fillRect(0, 0, Raycaster.CAMERA_WIDTH, Raycaster.CAMERA_WIDTH/2);
		
		//draws columns
		for(int i = 0; i < column_data.length; i++){
			double brightness = (column_data[i]/Raycaster.CAMERA_WIDTH)*0.8 + 0.2;
			g2D.setColor(new Color((float)brightness, 0 , 0));
			int v = (int)(Raycaster.CAMERA_WIDTH - column_data[i])/2;
			g2D.fillRect(i * Raycaster.COLUMN_WIDTH, v, Raycaster.COLUMN_WIDTH, (int)column_data[i]);
		}
	}
}
class MiniMap extends JPanel{
	
	double x;
	double y;
	double angle;
	boolean level[][];
	
	public MiniMap(double x_in, double y_in, double angle_in, boolean[][] level_in){
		x = x_in;
		y = y_in;
		angle = angle_in;
		level = level_in;
	}
	
	public void updatePosition(double x_in, double y_in, double angle_in){
		x = x_in;
		y = y_in;
		angle = angle_in;
	}
	
	@Override
	public Dimension getPreferredSize() {
	    return new Dimension(Raycaster.MINIMAP_WIDTH, Raycaster.MINIMAP_WIDTH);
	}
	
	@Override
	public void paintComponent(Graphics g){
		Graphics2D g2D = (Graphics2D) g;
		super.paintComponent(g2D);
		
		//draws grid
		g2D.setColor(new Color(200, 200, 200));
		int grid_width = Raycaster.MINIMAP_WIDTH/level.length;

		for(int grid_x = 0; grid_x < level.length; grid_x++){
			g2D.drawLine(grid_x * grid_width, 0, grid_x * grid_width, Raycaster.MINIMAP_WIDTH);
		}
		for(int grid_y = 0; grid_y < level[0].length; grid_y++){
			g2D.drawLine(0, grid_y * grid_width, Raycaster.MINIMAP_WIDTH, grid_y * grid_width);
		}
		
		//draws walls
		g2D.setColor(new Color(0, 0, 150));
		for(int grid_x = 0; grid_x < level.length; grid_x++){
			for(int grid_y = 0; grid_y < level[0].length; grid_y++){
				if(level[grid_x][grid_y] == true){
					g2D.fillRect(grid_x * grid_width, grid_y * grid_width, grid_width, grid_width);
				}
			}
		}
		
		//draws character
		g2D.setColor(new Color(255, 0, 0));
		g2D.fillRect((int)x - Raycaster.PLAYER_RADIUS, (int)y - Raycaster.PLAYER_RADIUS, Raycaster.PLAYER_RADIUS*2, Raycaster.PLAYER_RADIUS*2);
		g2D.setColor(new Color(0, 0, 255));
		int arrow_x = (int)(Math.cos(Math.toRadians(angle)) * Raycaster.ARROW_LENGTH);
		int arrow_y = (int)(Math.sin(Math.toRadians(angle)) * Raycaster.ARROW_LENGTH);
		g2D.drawLine((int)x, (int)y, (int)x + arrow_x, (int)y + arrow_y);
	}
}