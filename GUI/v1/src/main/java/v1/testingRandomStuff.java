package v1;

import java.util.ArrayList;

public class testingRandomStuff {

	public static void parse_input(String command) {
		
		System.out.println("Input string: " + command);

		int splits[] = new int[2];
		splits[0] = command.indexOf(',');
		splits[1] = command.substring(splits[0]+1).indexOf(',') + splits[0]+1;
		
		char opcode = command.charAt(splits[0]-1);
		int p;
		System.out.println(command.substring(splits[0]+1, splits[1]));
		try {
			p = Integer.valueOf(command.substring(splits[0]+1, splits[1]));
		}
		catch (Exception e) {System.out.println("err p1"); p = 0;}
		char param1 = (char) p;
		
		int p2;
		try {
			p2 = Integer.valueOf(command.substring(splits[1]+1));
		}
		catch (Exception e) {System.out.println("err p2"); p2 = 0;}
		char param2 = (char) p2;
		
		System.out.println("Indecies of commmas: " + splits[0] + " " + splits[1]);
		System.out.println(opcode + " " + p + " " + p2);
		System.out.println();
		
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

//		parse_input("d,0,90");
//		parse_input("d,0,100");
//		parse_input("d,90,0");
//		parse_input("d,0,90");
		ArrayList<Integer> r = new ArrayList<Integer>();
		r.add(1);
		r.add(3);
		
		r.removeAll(r);
		for(Integer i : r) {
			System.out.println(i);
		}
	}

}
