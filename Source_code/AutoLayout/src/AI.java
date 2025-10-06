import java.util.ArrayList;

import javax.swing.*;
//Класс для мультипоточного умного алгоритма раскладки
public class AI extends Thread {
	Product a;
	Detail d, previous;
	float distance;
	int accuracy, i, j, k, l, m, n;
	Form1 Main;
	JProgressBar Indicator;
	JLabel Label; JFrame Form;
	AI(Product A, Detail D, float D2, int K2, Form1 main, JProgressBar indicator, JLabel label, JFrame form) {
		a = A; d = D; previous = D; distance = D2; accuracy = K2;
		n = a.details.size(); Main = main; 
		Indicator = indicator; 
		Label = label; Form = form;
	}//алгоритм основан на генерации случайных раскладок и нахождении среди них самой эффективной.
	public void run() {
		try {//Раскладка деталей в случайном порядке со случайным вращением
			for(m = 0; m <= accuracy; m++) {
				if(Indicator.getValue() == 1000) break;
				if(m % 100 == 0) updateWindow(m);
				var used = new ArrayList<Detail>();
				Product t = new Product(a, 1);	
				for(n = t.details.size(), i = 0; i < n; i++) {
	    			if(i > 0) previous = d;
	    			k = (int)((Math.random()) * (n - i)); 
	    			d = t.details.get(k);
	    			d.normalize();
	    			if(d.Xmax() <= a.listHeight) {
	    				l = (int)(Math.random() * 4);
	    				d.rotate(90 * l);
	    			}
	    			else {
	    				l = (int)(Math.random() * 2);
	    				d.rotate(180 * l);
	    			}
	    			l = (int)(Math.random() * 2);
	    			if(l == 0) d.flipHorizontal();
	    			
	    			l = (int)(Math.random() * 2);
	    			if(l == 0) d.flipVertical();
	    			
	    			d.normalize();
	    			float H = previous.Ymax() + distance;
        			if(H + d.Ymax() > a.listHeight) {
        				l = (int)(Math.random() * 4);
        				d.rotate(90);
        				d.normalize();
        			}
	        		if(i > 0) {      					
	        			if(H + d.Ymax() <= a.listHeight) d.shiftY(H);
	        			d.shiftX(t.listWidth + distance); 
	        			//Сдвиг деталей влево и вниз, пока не будет пересения с другой деталью, либо с краем полотна.
	        			float minX = d.minX(), minY = d.minY();
		        		for(Boolean flag = true; flag && minX >= 0.02f; d.shiftX(-0.02f), minX -= 0.02f) 
		        			for(j = 0; j < i; j++) 
		            			if(d.intersects(used.get(j))) {
		            				flag = false; 
		            				d.shiftX(0.04f + distance);
		            				break;
		            			}	
		        		if(a.listWidth > 0 && d.Xmax() > a.listWidth) {
		        			t.listWidth = a.listWidth;
		        			break;
		        		}
		        		for(Boolean flag = true; flag && minY >= 0.02f; d.shiftY(-0.02f), minY -= 0.02f) 
		        			for(j = 0; j < i; j++) 
		            			if(d.intersects(used.get(j))) {
		            				flag = false;               				
		            				d.shiftY(0.04f + distance);
		            				break;
		            			}
	        		}		
	        		if(d.Xmax() > t.listWidth) t.listWidth = d.Xmax();		
	        		t.details.remove(d);
	        		used.add(d);   		 		
	        	}
				if(t.listWidth < a.listWidth || a.listWidth == 0) {
					a.listWidth = t.listWidth;
					for(var d : used) a.details.get(d.index).vertices = d.vertices;		
				}		
			}
			Main.updateFields(1);
			Thread.sleep(100);
			Form.dispose();
			SwingUtilities.invokeLater(() -> {
			    Main.setState(JFrame.MAXIMIZED_BOTH);
			    Main.setVisible(true);
			});
			
		}
		catch(Exception e) {}
	}
	private void updateWindow(int m) {
		int progress = (int) (m * 1000f / Math.max(1, accuracy));
        SwingUtilities.invokeLater(() -> {
            if (progress > Indicator.getValue()) Indicator.setValue(progress);
            Label.setText("ИИ-раскладка завершена на " + (progress / 10f) + "%");
        }); 
    }
}//Отбор наиболее эффективных раскладок из случайных является ЭЛЕМЕНТОМ искусственного интеллекта
