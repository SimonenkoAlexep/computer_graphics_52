import javax.swing.*;
import java.awt.*;

public class ColorPickerApp extends JFrame {

    private boolean internalUpdate = false;

    private JSlider rSlider, gSlider, bSlider;
    private JTextField rField, gField, bField;

    private JSlider cSlider, mSlider, ySlider, kSlider;
    private JTextField cField, mField, yField, kField;

    private JSlider hSlider, sSlider, vSlider;
    private JTextField hField, sField, vField;

    private JPanel colorDisplay;

    public ColorPickerApp() {
        super("Цветовой конвертер");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        ColorPickerPanel palette = new ColorPickerPanel(360, 100, this::updateFromColor);
        add(palette, BorderLayout.NORTH);

        JPanel blocksPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        blocksPanel.add(createRGBPanel());
        blocksPanel.add(createCMYKPanel());
        blocksPanel.add(createHSVPanel());
        add(blocksPanel, BorderLayout.CENTER);

        colorDisplay = new JPanel();
        colorDisplay.setPreferredSize(new Dimension(100, 100));
        colorDisplay.setBorder(BorderFactory.createTitledBorder("Текущий цвет"));
        add(colorDisplay, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }


    private JPanel createRGBPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        panel.setBorder(BorderFactory.createTitledBorder("RGB"));

        rSlider = createSlider(0, 255);
        gSlider = createSlider(0, 255);
        bSlider = createSlider(0, 255);

        rField = createTextField();
        gField = createTextField();
        bField = createTextField();

        panel.add(createLabeledSlider("R", rSlider, rField));
        panel.add(createLabeledSlider("G", gSlider, gField));
        panel.add(createLabeledSlider("B", bSlider, bField));

        addSync(rSlider, rField, () -> updateFromRGB());
        addSync(gSlider, gField, () -> updateFromRGB());
        addSync(bSlider, bField, () -> updateFromRGB());

        return panel;
    }

    private JPanel createCMYKPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1));
        panel.setBorder(BorderFactory.createTitledBorder("CMYK"));

        cSlider = createSlider(0, 100);
        mSlider = createSlider(0, 100);
        ySlider = createSlider(0, 100);
        kSlider = createSlider(0, 100);

        cField = createTextField();
        mField = createTextField();
        yField = createTextField();
        kField = createTextField();

        panel.add(createLabeledSlider("C", cSlider, cField));
        panel.add(createLabeledSlider("M", mSlider, mField));
        panel.add(createLabeledSlider("Y", ySlider, yField));
        panel.add(createLabeledSlider("K", kSlider, kField));

        addSync(cSlider, cField, () -> updateFromCMYK());
        addSync(mSlider, mField, () -> updateFromCMYK());
        addSync(ySlider, yField, () -> updateFromCMYK());
        addSync(kSlider, kField, () -> updateFromCMYK());

        return panel;
    }

    private JPanel createHSVPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        panel.setBorder(BorderFactory.createTitledBorder("HSV"));

        hSlider = createSlider(0, 360);
        sSlider = createSlider(0, 100);
        vSlider = createSlider(0, 100);

        hField = createTextField();
        sField = createTextField();
        vField = createTextField();

        panel.add(createLabeledSlider("H", hSlider, hField));
        panel.add(createLabeledSlider("S", sSlider, sField));
        panel.add(createLabeledSlider("V", vSlider, vField));

        addSync(hSlider, hField, () -> updateFromHSV());
        addSync(sSlider, sField, () -> updateFromHSV());
        addSync(vSlider, vField, () -> updateFromHSV());

        return panel;
    }


    private JSlider createSlider(int min, int max) {
        JSlider slider = new JSlider(min, max);
        slider.setMajorTickSpacing((max - min) / 5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        return slider;
    }

    private JTextField createTextField() {
        return new JTextField(4);
    }

    private JPanel createLabeledSlider(String label, JSlider slider, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel(label), BorderLayout.WEST);
        panel.add(slider, BorderLayout.CENTER);
        panel.add(field, BorderLayout.EAST);
        return panel;
    }

    private void addSync(JSlider slider, JTextField field, Runnable onChange) {
        slider.addChangeListener(e -> {
            if (!internalUpdate) {
                field.setText(String.valueOf(slider.getValue()));
                onChange.run();
            }
        });

        field.addActionListener(e -> {
            if (!internalUpdate) {
                try {
                    int val = Integer.parseInt(field.getText());
                    slider.setValue(val);
                    onChange.run();
                } catch (NumberFormatException ex) {
                    field.setText(String.valueOf(slider.getValue()));
                }
            }
        });
    }


    private void setColor(Color color, String sourceModel) {
        if (internalUpdate) return;
        internalUpdate = true;

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        // RGB
        if (!sourceModel.equals("RGB")) {
            rSlider.setValue(r);
            gSlider.setValue(g);
            bSlider.setValue(b);
            rField.setText(String.valueOf(r));
            gField.setText(String.valueOf(g));
            bField.setText(String.valueOf(b));
        }

        // CMYK
        if (!sourceModel.equals("CMYK")) {
            float[] cmyk = ColorConverter.rgbToCmyk(r, g, b);
            int C = Math.round(cmyk[0] * 100);
            int M = Math.round(cmyk[1] * 100);
            int Y = Math.round(cmyk[2] * 100);
            int K = Math.round(cmyk[3] * 100);

            cSlider.setValue(C);
            mSlider.setValue(M);
            ySlider.setValue(Y);
            kSlider.setValue(K);

            cField.setText(String.valueOf(C));
            mField.setText(String.valueOf(M));
            yField.setText(String.valueOf(Y));
            kField.setText(String.valueOf(K));
        }

        // HSV
        if (!sourceModel.equals("HSV")) {
            float[] hsv = ColorConverter.rgbToHsv(r, g, b);
            int H = Math.round(hsv[0]);
            int S = Math.round(hsv[1] * 100);
            int V = Math.round(hsv[2] * 100);

            hSlider.setValue(H);
            sSlider.setValue(S);
            vSlider.setValue(V);

            hField.setText(String.valueOf(H));
            sField.setText(String.valueOf(S));
            vField.setText(String.valueOf(V));
        }

        colorDisplay.setBackground(color);
        internalUpdate = false;
    }

    private void updateFromRGB() {
        setColor(new Color(rSlider.getValue(), gSlider.getValue(), bSlider.getValue()), "RGB");
    }

    private void updateFromCMYK() {
        float c = cSlider.getValue() / 100f;
        float m = mSlider.getValue() / 100f;
        float y = ySlider.getValue() / 100f;
        float k = kSlider.getValue() / 100f;

        int[] rgb = ColorConverter.cmykToRgb(c, m, y, k);
        setColor(new Color(rgb[0], rgb[1], rgb[2]), "CMYK");
    }

    private void updateFromHSV() {
        float h = hSlider.getValue();
        float s = sSlider.getValue() / 100f;
        float v = vSlider.getValue() / 100f;

        int[] rgb = ColorConverter.hsvToRgb(h, s, v);
        setColor(new Color(rgb[0], rgb[1], rgb[2]), "HSV");
    }

    private void updateFromColor(Color color) {
        setColor(color, "ColorPicker");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ColorPickerApp::new);
    }
}