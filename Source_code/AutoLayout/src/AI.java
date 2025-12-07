import java.util.ArrayList;
import javax.swing.*;


// Класс для умного алгоритма раскладки
public class AI extends Thread {
    Product a, b;
    Detail d, previous;
    float distance;
    int accuracy, i, j, k, l, m, n;
    Form1 Main;
    JProgressBar Indicator;
    JLabel Label;
    JFrame Form;

    AI(Product A, Detail D, float D2, int K2, Form1 main, JProgressBar indicator, JLabel label, JFrame form) {
        a = new Product(A, 1);
        d = D;
        previous = D;
        distance = D2;
        accuracy = K2;
        n = a.details.size();
        Main = main;
        Indicator = indicator;
        Label = label;
        Form = form;
    }
    // Алгоритм основан на генерации случайных раскладок и нахождении самой эффективной
    public void run() {
    	
        try {
        	randomGeneration();
        } catch (Exception e) {
        	
        }
        finish();
    }
    private void randomGeneration() 
    {
    	// Раскладка деталей в случайном порядке со случайным вращением
        for (m = 0; m <= accuracy; m++) {
            if (Indicator != null && Indicator.getValue() == 1000) break;
            if (m % 1000 == 0) updateWindow(m);

            var used = new ArrayList<Detail>();
            Product t = new Product(a, 1);

            for (n = t.details.size(), i = 0; i < n; i++) {
                if (i > 0) previous = d;
                k = (int) (Math.random() * (n - i));
                d = t.details.get(k);
                d.normalize();	
                float H = previous.Ymax() + distance;

                if (i > 0) {
                    if (H + d.Ymax() + distance <= a.listHeight) d.shiftY(H);
                    d.shiftX(t.listWidth + distance);
                    d.packX(used, 0.03f, distance);
                    if (a.listWidth > 0 && d.Xmax() > a.listWidth) {
                        t.listWidth = a.listWidth;
                        break;
                    }   
                    d.packY(used, 0.03f, distance);	    
                }
                if (d.Xmax() > t.listWidth) t.listWidth = d.Xmax();
                t.details.remove(d);
                used.add(d);
            }
            if ((t.listWidth < a.listWidth || a.listWidth == 0) && !t.collision()) {
                a.listWidth = t.listWidth;
                for (var d : used) a.details.get(d.index).vertices = d.vertices;
            }
        }
    }
    private void finish() {
    	try {
    		SwingUtilities.invokeLater(() -> {
                a.rascladMode = true;              
                if (Main == null) Main = new Form1(a, a.filePath);
    			Main.scale.setText("Масштаб: "+ Math.round(a.scaling * 100) + "%");
    			float listWidth = Main.product.listWidth;
                Main.product = a;
                Main.setVisible(true);
                if (Form != null) Form.dispose();
            });
        }
        catch (Exception e) {
            finish();
        }	
    }
    // Безопасное обновление прогресс-бара и текста
    private void updateWindow(int m) {
        if (Indicator == null || Label == null) return;

        int progress = (int) (m * 1000f / Math.max(1, accuracy));
        SwingUtilities.invokeLater(() -> {
            if (Indicator != null && progress > Indicator.getValue()) {
                Indicator.setValue(progress);
            }
            if (Label != null) {
                Label.setText("Раскладка завершена на " + (progress / 10f) + "%");
            }
        });
    }
}
