import javax.swing.*;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

import javax.swing.text.PlainDocument;

public class NumericField extends JTextField {
	int index;//Числовое поле, через которое можно редактировать координаты точек деталей
	NumericField(int i) {
		index = i;//Номер поля в правой панели с координатами
		setDocument(new PlainDocument() {
        	@Override
        	public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        		if (str == null) return;
        		try {//Попытка парсинга текста в числовое значение координаты
        			if(str.endsWith(".")) str += '0';
        			Float.parseFloat(str);
        		} catch (NumberFormatException ex) { return; }	
        		super.insertString(offs, str, a);
        	}
        });
	}
}