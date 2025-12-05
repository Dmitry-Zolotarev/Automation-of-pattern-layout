import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

public class Detail {//Класс детали
	List<Dot> vertices = new ArrayList<>();
	String name = "Новая деталь";
	int index, current = 0; 
	float H, S;
	Boolean onRasclad = true;
	Detail(String s) { if(s != null && s.length() > 0) name = s;}
	Detail(Detail other) {
		onRasclad = other.onRasclad; current = other.current;
		name = other.name; index = other.index; H = other.H;	
    	for(var v : other.vertices) vertices.add(new Dot(v.X, v.Y));
	}
	Detail() { }
	public void addVertex(Dot vertex) {
	    // Проверка на дубликаты
	    for (var v : vertices) 
	    {
	        if (v.intX(100) == vertex.intX(100) && v.intY(100) == vertex.intY(100))
	            return;
	    }
	    // Если список пуст — добавляем первую точку
	    if (vertices.isEmpty()) 
	    {
	        vertices.add(vertex);
	        current = 0;
	    } else {
	        // Смещаем индекс фокуса, не выходя за пределы списка
	        current = Math.min(current + 1, vertices.size());
	        vertices.add(current, vertex);
	    }
	}
	public void generateDots() 
	{
		int n = (int)(Math.random() * 8) + 3;
		for(int i = 0; i < n; i++) 
		{
			float x = (float)Math.random(), y = (float)Math.random();
			vertices.add(new Dot(x / 2, y / 2));
		}
		normalize();
		//for(int i = 0; i < vertices.size(); i++) vertices.get(i).normalize();
		
	}
	public void normalize() 
	{
		float minX = minX(), minY = minY();
		for(int i = 0; i < vertices.size(); i++) {
			vertices.get(i).X -= minX;
			vertices.get(i).Y -= minY;
		}	
	}
	public float Xmax() 
	{
		float max = 0;
		for(var v: vertices) if(v.X > max) max = v.X;
		return max;
	};
	public float Ymax() 
	{
		float max = 0;
		for(var v: vertices) if(v.Y > max) max = v.Y;
		return max;
	};
	public float minX() 
	{
		float min = 9;
		for(var v: vertices) if(v.X < min) min = v.X;
		return min;
	};
	public float minY() 
	{
		float min = 9;
		for(var v: vertices) if(v.Y < min) min = v.Y;
		return min;
	};
	//Сдвиг деталей
	public void shiftX(float shift) 
	{
		for(int i = 0; i < vertices.size(); i++) vertices.get(i).X += shift;
	} 
	public void shiftY(float shift) 
	{
		for(int i = 0; i < vertices.size(); i++) vertices.get(i).Y += shift;
	}
	public void scale(float k) 
	{
		for(int i = 0; i < vertices.size(); i++) 
		{
			vertices.get(i).X *= k;
			vertices.get(i).Y *= k;
		}	
		normalize();
		shiftX(0.1f); shiftY(0.1f); 
	};
	public Boolean intersects(Detail other) 
	{
	    int n1 = vertices.size(), n2 = other.vertices.size();
	    int xPoints[] = new int[n1];
	    int yPoints[] = new int[n1];
	    int xPoints2[] = new int[n2];
	    int yPoints2[] = new int[n2];
	    // 2) Проверка наложения фигур 
	    for (int i = 0; i < n1; i++) {
	        xPoints[i] = vertices.get(i).intX(10000);
	        yPoints[i] = vertices.get(i).intY(10000);
	    }
	    for (int i = 0; i < n2; i++) {
	        xPoints2[i] = other.vertices.get(i).intX(10000);
	        yPoints2[i] = other.vertices.get(i).intY(10000);
	    }
	    var figure = new java.awt.Polygon(xPoints, yPoints, n1);
	    var figure2 = new java.awt.Polygon(xPoints2, yPoints2, n2);

	    for (int i = 0; i < n2; i++)
	        if (figure.contains(xPoints2[i], yPoints2[i])) return true;
	    for (int i = 0; i < n1; i++)
	        if (figure2.contains(xPoints[i], yPoints[i])) return true;   
	    // 2) Проверка пересечения отрезков 
	    for (int i = 1; i < n1; i++)
	        for (int j = 1; j < n2; j++)
	            if (doIntersect(vertices.get(i - 1), vertices.get(i), other.vertices.get(j - 1), other.vertices.get(j)))
	                return true;
	    return false;
	}

	private boolean doIntersect(Dot p1, Dot q1, Dot p2, Dot q2) 
	{
		return cross(p1, q1, p2) != cross(p1, q1, q2) && cross(p2, q2, p1) != cross(p2, q2, q1);
    }
	private int cross(Dot p, Dot q, Dot r) 
	{
        double Z = (r.X - q.X) * (q.Y - p.Y) - (q.X - p.X) * (r.Y - q.Y);
        return (Z > 0) ? 1 : 2;//Z - длина векторного произведения отрезков по оси Z.
    }
	public int S() {
        if (vertices.size() < 3) return 0;   
        float area = 0;
        int n = vertices.size();
        for (int i = 0; i < n; i++) 
            area += vertices.get(i).X * vertices.get((i + 1) % n).Y - vertices.get((i + 1) % n).X * vertices.get(i).Y;
        return (int)(Math.abs(area) * 5000);
    }
	public void flipHorizontal() {
		float centerX = 0;
		for(var v : vertices) centerX += v.X;
		centerX /= vertices.size();
		for(int i = 0; i < vertices.size(); i++) 
			vertices.get(i).X = -vertices.get(i).X + 2 * centerX;
	}
	public void flipVertical() {
		float centerY = 0;
		for(var v : vertices) centerY += v.Y;
		centerY /= vertices.size();
		for(int i = 0; i < vertices.size(); i++) 
			vertices.get(i).Y = -vertices.get(i).Y + 2 * centerY;
	}	
	public void rotate(double angle) 
	{//Вращение на угол в градусах
		angle = Math.toRadians(angle);
		Dot center = new Dot(0, 0);
		for(var v : vertices) {
			center.X += v.X;
			center.Y += v.Y;
		}
		center.X /= vertices.size();
		center.Y /= vertices.size();
		for(int i = 0; i < vertices.size(); i++) {
			float dx = vertices.get(i).X - center.X, dy = vertices.get(i).Y - center.Y,
		     	  newX = (float)(dx * Math.cos(angle) - dy * Math.sin(angle)), 
		     	  newY = (float)(dx * Math.sin(angle) + dy * Math.cos(angle));     	  
		    vertices.get(i).X = newX + center.X;
		    vertices.get(i).Y = newY + center.Y;	    
		    float minX = minX(), minY = minY();
		}
	}
}
