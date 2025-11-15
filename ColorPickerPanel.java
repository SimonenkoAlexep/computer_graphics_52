import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ColorPickerPanel extends JPanel {

    public interface ColorSelectionListener {
        void colorSelected(Color color);
    }

    private final int width;
    private final int height;
    private final ColorSelectionListener listener;
    private final int cellSize = 3; // размер "пикселя" палитры

    public ColorPickerPanel(int width, int height, ColorSelectionListener listener) {
        this.width = width;
        this.height = height;
        this.listener = listener;

        setPreferredSize(new Dimension(width, height));
        setBorder(BorderFactory.createTitledBorder("Палитра"));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                float hue = (float) x / width;
                float saturation = 1f - (float) y / height;
                if (saturation < 0) saturation = 0;
                if (saturation > 1) saturation = 1;
                Color c = Color.getHSBColor(hue, saturation, 1f);
                if (listener != null) listener.colorSelected(c);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int x = 0; x < width; x += cellSize) {
            for (int y = 0; y < height; y += cellSize) {
                float hue = (float) x / width;
                float saturation = 1f - (float) y / height;
                Color color = Color.getHSBColor(hue, saturation, 1f);
                g.setColor(color);
                g.fillRect(x, y, cellSize, cellSize);
            }
        }
    }
}