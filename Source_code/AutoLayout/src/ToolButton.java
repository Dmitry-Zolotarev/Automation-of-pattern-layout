import java.awt.Dimension;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.*;

public class ToolButton extends JButton{//Конструктор кнопки со значком и подсказкой
	ToolButton(String filePath, String toolTip) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
            if (is != null) {
                ImageIcon icon = new ImageIcon(ImageIO.read(is));
                setIcon(icon);
                setPreferredSize(new Dimension(24, 24));
                setBorder(BorderFactory.createEmptyBorder());
                setToolTipText(toolTip);
            }
        } catch (Exception e) {}
    }
}
