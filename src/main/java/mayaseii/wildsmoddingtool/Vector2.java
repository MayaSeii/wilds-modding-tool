package mayaseii.wildsmoddingtool;

public class Vector2
{
    public final static Vector2 Zero = new Vector2();

    public double x;

    public double y;

    public Vector2()
    {
        this.x = 0;
        this.y = 0;
    }

    public Vector2(int x, int y)
    {
        this.x = x;
        this.y = y;
    }
}
