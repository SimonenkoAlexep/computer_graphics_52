import javax.swing.*;
import java.awt.*;

public class ColorPickerApp extends JFrame {
    private final JPanel previewPanel;
    private final JTextField rgbField, hsvField, cmykField;

    private final ComponentControl rCtrl, gCtrl, bCtrl;
    private final ComponentControl hCtrl, sCtrl, vCtrl;
    private final ComponentControl cCtrl, mCtrl, yCtrl, kCtrl;

    private boolean updating = false;

    public ColorPickerApp() {
        super("RGB / HSV / CMYK Color Picker");

        previewPanel = new JPanel();
        previewPanel.setPreferredSize(new Dimension(120, 120));

        rgbField = new JTextField(25);
        hsvField = new JTextField(25);
        cmykField = new JTextField(25);
        rgbField.setEditable(false);
        hsvField.setEditable(false);
        cmykField.setEditable(false);

        // Контролы
        rCtrl = new ComponentControl("R", 0, 255);
        gCtrl = new ComponentControl("G", 0, 255);
        bCtrl = new ComponentControl("B", 0, 255);

        hCtrl = new ComponentControl("H", 0, 360);
        sCtrl = new ComponentControl("S", 0, 100);
        vCtrl = new ComponentControl("V", 0, 100);

        cCtrl = new ComponentControl("C", 0, 100);
        mCtrl = new ComponentControl("M", 0, 100);
        yCtrl = new ComponentControl("Y", 0, 100);
        kCtrl = new ComponentControl("K", 0, 100);

        // Слушатели
        rCtrl.addListener(() -> updateFromRGB(new Color(rCtrl.getValue(), gCtrl.getValue(), bCtrl.getValue())));
        gCtrl.addListener(() -> updateFromRGB(new Color(rCtrl.getValue(), gCtrl.getValue(), bCtrl.getValue())));
        bCtrl.addListener(() -> updateFromRGB(new Color(rCtrl.getValue(), gCtrl.getValue(), bCtrl.getValue())));

        hCtrl.addListener(() -> {
            if (!updating) {
                float h = hCtrl.getValue();
                float s = sCtrl.getValue() / 100f;
                float v = vCtrl.getValue() / 100f;
                Color c = Color.getHSBColor(h / 360f, s, v);
                updateFromRGB(c);
            }
        });
        sCtrl.addListener(hCtrl::trigger);
        vCtrl.addListener(hCtrl::trigger);

        cCtrl.addListener(() -> {
            if (!updating) {
                double c = cCtrl.getValue() / 100.0;
                double m = mCtrl.getValue() / 100.0;
                double y = yCtrl.getValue() / 100.0;
                double k = kCtrl.getValue() / 100.0;
                int r = (int) Math.round(255 * (1 - c) * (1 - k));
                int g = (int) Math.round(255 * (1 - m) * (1 - k));
                int b = (int) Math.round(255 * (1 - y) * (1 - k));
                updateFromRGB(new Color(r, g, b));
            }
        });
        mCtrl.addListener(cCtrl::trigger);
        yCtrl.addListener(cCtrl::trigger);
        kCtrl.addListener(cCtrl::trigger);

        JButton chooseButton = new JButton("Выбрать цвет...");
        chooseButton.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, "Выбор цвета", previewPanel.getBackground());
            if (chosen != null) updateFromRGB(chosen);
        });

        JPanel rgbPanel = new JPanel(new GridLayout(3, 1));
        rgbPanel.setBorder(BorderFactory.createTitledBorder("RGB"));
        rgbPanel.add(rCtrl.panel);
        rgbPanel.add(gCtrl.panel);
        rgbPanel.add(bCtrl.panel);

        JPanel hsvPanel = new JPanel(new GridLayout(3, 1));
        hsvPanel.setBorder(BorderFactory.createTitledBorder("HSV"));
        hsvPanel.add(hCtrl.panel);
        hsvPanel.add(sCtrl.panel);
        hsvPanel.add(vCtrl.panel);

        JPanel cmykPanel = new JPanel(new GridLayout(4, 1));
        cmykPanel.setBorder(BorderFactory.createTitledBorder("CMYK"));
        cmykPanel.add(cCtrl.panel);
        cmykPanel.add(mCtrl.panel);
        cmykPanel.add(yCtrl.panel);
        cmykPanel.add(kCtrl.panel);

        JPanel fields = new JPanel(new GridLayout(3, 1));
        fields.add(rgbField);
        fields.add(hsvField);
        fields.add(cmykField);

        JPanel sliders = new JPanel(new GridLayout(1, 3, 10, 10));
        sliders.add(rgbPanel);
        sliders.add(hsvPanel);
        sliders.add(cmykPanel);

        setLayout(new BorderLayout(10, 10));
        add(previewPanel, BorderLayout.WEST);
        add(sliders, BorderLayout.CENTER);
        add(fields, BorderLayout.EAST);
        add(chooseButton, BorderLayout.SOUTH);

        updateFromRGB(Color.RED);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void updateFromRGB(Color c) {
        updating = true;
        previewPanel.setBackground(c);

        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();

        rgbField.setText(String.format("RGB: (%d, %d, %d)", r, g, b));
        rCtrl.setValue(r);
        gCtrl.setValue(g);
        bCtrl.setValue(b);

        float[] hsv = Color.RGBtoHSB(r, g, b, null);
        int h = Math.round(hsv[0] * 360);
        int s = Math.round(hsv[1] * 100);
        int v = Math.round(hsv[2] * 100);
        hsvField.setText(String.format("HSV: (%d°, %d%%, %d%%)", h, s, v));
        hCtrl.setValue(h);
        sCtrl.setValue(s);
        vCtrl.setValue(v);

        double rn = r / 255.0, gn = g / 255.0, bn = b / 255.0;
        double k = 1 - Math.max(rn, Math.max(gn, bn));
        double cmykC, cmykM, cmykY;
        if (k < 1.0) {
            cmykC = (1 - rn - k) / (1 - k);
            cmykM = (1 - gn - k) / (1 - k);
            cmykY = (1 - bn - k) / (1 - k);
        } else {
            cmykC = cmykM = cmykY = 0;
        }
        int ci = (int) Math.round(cmykC * 100);
        int mi = (int) Math.round(cmykM * 100);
        int yi = (int) Math.round(cmykY * 100);
        int ki = (int) Math.round(k * 100);
        cmykField.setText(String.format("CMYK: (%d%%, %d%%, %d%%, %d%%)", ci, mi, yi, ki));
        cCtrl.setValue(ci);
        mCtrl.setValue(mi);
        yCtrl.setValue(yi);
        kCtrl.setValue(ki);

        updating = false;
    }

    // Класс: слайдер + поле
    private static class ComponentControl {
        final JPanel panel;
        final JSlider slider;
        final JTextField field;
        private Runnable listener;

        ComponentControl(String name, int min, int max) {
            panel = new JPanel(new BorderLayout());
            slider = new JSlider(min, max);
            field = new JTextField(4);
            panel.add(new JLabel(name), BorderLayout.WEST);
            panel.add(slider, BorderLayout.CENTER);
            panel.add(field, BorderLayout.EAST);

            slider.addChangeListener(e -> {
                if (!slider.getValueIsAdjusting()) {
                    field.setText(String.valueOf(slider.getValue()));
                    if (listener != null) listener.run();
                }
            });
            field.addActionListener(e -> {
                try {
                    int val = Integer.parseInt(field.getText());
                    val = Math.max(min, Math.min(max, val));
                    slider.setValue(val);
                    if (listener != null) listener.run();
                } catch (NumberFormatException ignored) {}
            });
        }

        void addListener(Runnable r) { this.listener = r; }
        void trigger() { if (listener != null) listener.run(); }
        int getValue() { return slider.getValue(); }
        void setValue(int v) {
            slider.setValue(v);
            field.setText(String.valueOf(v));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ColorPickerApp::new);
    }
}
