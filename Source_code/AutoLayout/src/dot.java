//Класс для точки в нормализованных координатах
public class dot {
	float X, Y;
	public dot(float x, float y) 
	{
		this.X = x; this.Y = y; 
	}
	public int intX(float height) {
		return Math.round(X * height);
	}//Масштабирование координаты X под заланный размер.
	public int intY(float height) {
		return Math.round(Y * height);
	}//Масштабирование координаты Y под заланный размер.
}
//Координаты хранятся в нормализованном виде для возможности масштабирования и отвязки от размеров окна.
