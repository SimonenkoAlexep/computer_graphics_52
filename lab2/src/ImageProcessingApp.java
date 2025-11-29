import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Обработка изображений — Морфология и Сегментация (Sobel, Laplacian, упрощённый Canny)
 * Чистый Java, без OpenCV. Подробные пояснения в коде.
 */
public class ImageProcessingApp extends JFrame {
    private BufferedImage original;
    private BufferedImage processed;

    private final JLabel originalLabel = new JLabel();
    private final JLabel processedLabel = new JLabel();

    // Верхние кнопки
    private final JButton loadBtn = new JButton("Загрузить изображение");
    private final JButton saveBtn = new JButton("Сохранить результат");
    private final JButton resetBtn = new JButton("Сброс");

    // Морфология
    private final JButton morphBtn = new JButton("Применить морфологию");
    private final JComboBox<String> morphOp = new JComboBox<>(new String[]{"Эрозия","Дилатация","Открытие","Закрытие"});
    private final JComboBox<String> seShape = new JComboBox<>(new String[]{"Крест","Прямоугольник","Эллипс"});
    private final JSpinner seSize = new JSpinner(new SpinnerNumberModel(3, 1, 31, 2)); // нечётные лучше (центр ядра)

    // Сегментация
    private final JButton segBtn = new JButton("Применить сегментацию");
    private final JComboBox<String> segOp = new JComboBox<>(new String[]{"Sobel","Laplacian","Canny"});
    private final JSpinner cannyLow = new JSpinner(new SpinnerNumberModel(40, 0, 255, 1));
    private final JSpinner cannyHigh = new JSpinner(new SpinnerNumberModel(100, 1, 255, 1));

    public ImageProcessingApp() {
        super("Обработка изображений — Морфология и Сегментация");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Верхняя панель
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(loadBtn);
        top.add(saveBtn);
        top.add(resetBtn);
        add(top, BorderLayout.NORTH);

        // Центр: оригинал и результат
        JPanel center = new JPanel(new GridLayout(1,2));
        originalLabel.setHorizontalAlignment(JLabel.CENTER);
        processedLabel.setHorizontalAlignment(JLabel.CENTER);
        center.add(new JScrollPane(originalLabel));
        center.add(new JScrollPane(processedLabel));
        add(center, BorderLayout.CENTER);

        // Нижние панели управления
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        // Панель морфологии
        JPanel mPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        mPanel.setBorder(BorderFactory.createTitledBorder("Морфологическая обработка"));
        mPanel.add(new JLabel("Операция:"));
        mPanel.add(morphOp);
        mPanel.add(new JLabel("SE форма:"));
        mPanel.add(seShape);
        mPanel.add(new JLabel("Размер:"));
        mPanel.add(seSize);
        mPanel.add(morphBtn);
        controls.add(mPanel);

        // Панель сегментации
        JPanel sPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sPanel.setBorder(BorderFactory.createTitledBorder("Сегментация: линии, точки, перепады яркости"));
        sPanel.add(new JLabel("Метод:"));
        sPanel.add(segOp);
        sPanel.add(new JLabel("Canny low/high:"));
        sPanel.add(cannyLow);
        sPanel.add(cannyHigh);
        sPanel.add(segBtn);
        controls.add(sPanel);

        add(controls, BorderLayout.SOUTH);

        attachListeners();
        pack();
        setSize(1200, 750);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void attachListeners() {
        loadBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    File f = fc.getSelectedFile();
                    original = ImageIO.read(f);
                    processed = deepCopy(original);
                    updateViews();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка при чтении файла: " + ex.getMessage());
                }
            }
        });

        saveBtn.addActionListener(e -> {
            if (processed == null) return;
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    File out = fc.getSelectedFile();
                    ImageIO.write(processed, "PNG", out);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка при сохранении: " + ex.getMessage());
                }
            }
        });

        resetBtn.addActionListener(e -> {
            if (original != null) {
                processed = deepCopy(original);
                updateViews();
            }
        });

        morphBtn.addActionListener(e -> {
            if (original == null) return;
            String op = (String) morphOp.getSelectedItem();
            String shape = (String) seShape.getSelectedItem();
            int size = (Integer) seSize.getValue();
            if (size % 2 == 0) size++; // обеспечиваем центр
            processed = morphology(grayImage(original), op, shape, size);
            updateViews();
        });

        segBtn.addActionListener(e -> {
            if (original == null) return;
            String op = (String) segOp.getSelectedItem();
            BufferedImage gray = grayImage(original);
            if ("Sobel".equals(op)) {
                processed = sobelGradient(gray);
            } else if ("Laplacian".equals(op)) {
                processed = laplacian(gray);
            } else {
                int low = (Integer) cannyLow.getValue();
                int high = (Integer) cannyHigh.getValue();
                processed = canny(gray, low, high);
            }
            updateViews();
        });
    }

    private void updateViews() {
        if (original != null) originalLabel.setIcon(new ImageIcon(scaleToLabel(original, originalLabel)));
        if (processed != null) processedLabel.setIcon(new ImageIcon(scaleToLabel(processed, processedLabel)));
    }

    private Image scaleToLabel(BufferedImage img, JLabel label) {
        int w = img.getWidth();
        int h = img.getHeight();
        int lw = Math.max(200, getWidth()/2 - 50);
        int lh = Math.max(200, getHeight() - 250);
        double scale = Math.min((double)lw/w, (double)lh/h);
        if (scale >= 1.0) return img;
        return img.getScaledInstance((int)(w*scale), (int)(h*scale), Image.SCALE_SMOOTH);
    }

    // ---------- Вспомогательные функции ----------

    private static BufferedImage deepCopy(BufferedImage bi) {
        BufferedImage copy = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();
        return copy;
    }

    private static int gray(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int)(0.299*r + 0.587*g + 0.114*b);
    }

    private static int clamp8(int v){ return v < 0 ? 0 : (v > 255 ? 255 : v); }

    private static BufferedImage grayImage(BufferedImage src){
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w,h,BufferedImage.TYPE_BYTE_GRAY);
        for(int y=0;y<h;y++){
            for(int x=0;x<w;x++){
                int g = gray(src.getRGB(x,y));
                int rgb = (g<<16)|(g<<8)|g;
                out.setRGB(x,y,rgb);
            }
        }
        return out;
    }

    // ---------- Морфология ----------

    /**
     * Генерация структурирующего элемента в виде булевой маски size×size.
     * Поддержка: Крест, Прямоугольник, Эллипс (аппроксимированный круг).
     */
    private static boolean[][] makeSE(String shape, int size){
        boolean[][] se = new boolean[size][size];
        int c = size/2;
        switch (shape) {
            case "Прямоугольник":
                for(int j=0;j<size;j++) for(int i=0;i<size;i++) se[j][i] = true;
                break;
            case "Крест":
                for(int j=0;j<size;j++) se[j][c] = true;
                for(int i=0;i<size;i++) se[c][i] = true;
                break;
            case "Эллипс":
            default:
                double r = c + 0.5;
                for(int j=0;j<size;j++){
                    for(int i=0;i<size;i++){
                        double dx = i - c;
                        double dy = j - c;
                        se[j][i] = (dx*dx + dy*dy) <= r*r; // круглая маска
                    }
                }
                break;
        }
        return se;
    }

    /**
     * Эрозия бинарно/градационная: минимум по пикселям, покрытым SE.
     */
    private static BufferedImage erode(BufferedImage src, boolean[][] se){
        int w = src.getWidth(), h = src.getHeight();
        int k = se.length, half = k/2;
        BufferedImage out = new BufferedImage(w,h,BufferedImage.TYPE_BYTE_GRAY);
        for(int y=0;y<h;y++){
            for(int x=0;x<w;x++){
                int m = 255;
                for(int j=0;j<k;j++){
                    for(int i=0;i<k;i++){
                        if(!se[j][i]) continue;
                        int xx = Math.min(Math.max(x + i - half, 0), w-1);
                        int yy = Math.min(Math.max(y + j - half, 0), h-1);
                        int g = src.getRGB(xx,yy) & 0xFF;
                        if(g < m) m = g;
                    }
                }
                int rgb = (m<<16)|(m<<8)|m;
                out.setRGB(x,y,rgb);
            }
        }
        return out;
    }

    /**
     * Дилатация: максимум по пикселям, покрытым SE.
     */
    private static BufferedImage dilate(BufferedImage src, boolean[][] se){
        int w = src.getWidth(), h = src.getHeight();
        int k = se.length, half = k/2;
        BufferedImage out = new BufferedImage(w,h,BufferedImage.TYPE_BYTE_GRAY);
        for(int y=0;y<h;y++){
            for(int x=0;x<w;x++){
                int M = 0;
                for(int j=0;j<k;j++){
                    for(int i=0;i<k;i++){
                        if(!se[j][i]) continue;
                        int xx = Math.min(Math.max(x + i - half, 0), w-1);
                        int yy = Math.min(Math.max(y + j - half, 0), h-1);
                        int g = src.getRGB(xx,yy) & 0xFF;
                        if(g > M) M = g;
                    }
                }
                int rgb = (M<<16)|(M<<8)|M;
                out.setRGB(x,y,rgb);
            }
        }
        return out;
    }

    /**
     * Обёртка морфологии: Эрозия, Дилатация, Открытие (Эрозия→Дилатация), Закрытие (Дилатация→Эрозия).
     */
    private BufferedImage morphology(BufferedImage srcGray, String op, String shape, int size) {
        boolean[][] se = makeSE(shape, size);
        switch (op) {
            case "Эрозия":
                return erode(srcGray, se);
            case "Дилатация":
                return dilate(srcGray, se);
            case "Открытие":
                return dilate(erode(srcGray, se), se);
            case "Закрытие":
                return erode(dilate(srcGray, se), se);
            default:
                return srcGray;
        }
    }

    // ---------- Сегментация ----------

    /**
     * Собель: величина градиента sqrt(gx^2 + gy^2) с нормализацией на 0..255.
     * Ядра:
     * Gx = [-1 0 1; -2 0 2; -1 0 1], Gy = [ 1  2  1; 0 0 0; -1 -2 -1]
     */
    private BufferedImage sobelGradient(BufferedImage src){
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w,h,BufferedImage.TYPE_BYTE_GRAY);

        int[][] gxK = {{-1,0,1},{-2,0,2},{-1,0,1}};
        int[][] gyK = {{ 1,2,1},{ 0,0,0},{-1,-2,-1}};

        double maxMag = 1.0; // для нормализации
        double[][] mag = new double[h][w];

        for(int y=1;y<h-1;y++){
            for(int x=1;x<w-1;x++){
                int gx=0, gy=0;
                for(int j=-1;j<=1;j++){
                    for(int i=-1;i<=1;i++){
                        int g = src.getRGB(x+i,y+j) & 0xFF;
                        gx += gxK[j+1][i+1] * g;
                        gy += gyK[j+1][i+1] * g;
                    }
                }
                double m = Math.sqrt(gx*gx + gy*gy);
                mag[y][x] = m;
                if (m > maxMag) maxMag = m;
            }
        }

        // нормализация и запись
        for(int y=0;y<h;y++){
            for(int x=0;x<w;x++){
                int v = (int) Math.round(255.0 * (mag[y][x] / maxMag));
                v = clamp8(v);
                int rgb = (v<<16)|(v<<8)|v;
                out.setRGB(x,y,rgb);
            }
        }
        return out;
    }

    /**
     * Лапласиан (ядро 3x3): выделение точек резкого перепада яркости.
     */
    private BufferedImage laplacian(BufferedImage src){
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w,h,BufferedImage.TYPE_BYTE_GRAY);
        int[][] k = {{0,1,0},{1,-4,1},{0,1,0}}; // классическое ядро

        for(int y=1;y<h-1;y++){
            for(int x=1;x<w-1;x++){
                int acc = 0;
                for(int j=-1;j<=1;j++){
                    for(int i=-1;i<=1;i++){
                        int g = src.getRGB(x+i,y+j) & 0xFF;
                        acc += k[j+1][i+1] * g;
                    }
                }
                int v = clamp8(Math.abs(acc));
                int rgb = (v<<16)|(v<<8)|v;
                out.setRGB(x,y,rgb);
            }
        }
        return out;
    }

    /**
     * Упрощённый Canny:
     * 1) Лёгкое сглаживание (бокс 3×3)
     * 2) Собель → градиент + направление
     * 3) Non-Maximum Suppression (NMS)
     * 4) Двойной порог (low/high)
     * Примечание: без «edge tracking by hysteresis», но для учебных целей достаточно.
     */
    private BufferedImage canny(BufferedImage src, int low, int high){
        BufferedImage smooth = boxBlur(src, 3);

        // 1) Собель: величина и направление
        int w = src.getWidth(), h = src.getHeight();
        double[][] mag = new double[h][w];
        double[][] dir = new double[h][w]; // угол в градусах (0..180)
        int[][] gxK = {{-1,0,1},{-2,0,2},{-1,0,1}};
        int[][] gyK = {{ 1,2,1},{ 0,0,0},{-1,-2,-1}};

        double maxMag = 1.0;

        for(int y=1;y<h-1;y++){
            for(int x=1;x<w-1;x++){
                int gx=0, gy=0;
                for(int j=-1;j<=1;j++){
                    for(int i=-1;i<=1;i++){
                        int g = smooth.getRGB(x+i,y+j) & 0xFF;
                        gx += gxK[j+1][i+1] * g;
                        gy += gyK[j+1][i+1] * g;
                    }
                }
                double m = Math.sqrt(gx*gx + gy*gy);
                mag[y][x] = m;
                if (m > maxMag) maxMag = m;
                double angle = Math.toDegrees(Math.atan2(gy, gx));
                if (angle < 0) angle += 180;
                dir[y][x] = angle;
            }
        }

        // 2) NMS: подавление немаксимумов вдоль направления
        double[][] nms = new double[h][w];
        for(int y=1;y<h-1;y++){
            for(int x=1;x<w-1;x++){
                double angle = dir[y][x];
                double m = mag[y][x];
                // квантуем направление на 0, 45, 90, 135
                int sector;
                if ((angle >= 0 && angle < 22.5) || (angle >= 157.5 && angle <= 180)) sector = 0;
                else if (angle >= 22.5 && angle < 67.5) sector = 45;
                else if (angle >= 67.5 && angle < 112.5) sector = 90;
                else sector = 135;

                double m1=0, m2=0;
                switch (sector) {
                    case 0:   m1 = mag[y][x-1]; m2 = mag[y][x+1]; break;         // горизонт
                    case 45:  m1 = mag[y-1][x+1]; m2 = mag[y+1][x-1]; break;     // диагональ /
                    case 90:  m1 = mag[y-1][x]; m2 = mag[y+1][x]; break;         // вертикаль
                    case 135: m1 = mag[y-1][x-1]; m2 = mag[y+1][x+1]; break;     // диагональ \
                }
                nms[y][x] = (m >= m1 && m >= m2) ? m : 0.0;
            }
        }

        // 3) Двойной порог. Нормализуем к 0..255 и применяем пороги.
        BufferedImage out = new BufferedImage(w,h,BufferedImage.TYPE_BYTE_GRAY);
        for(int y=0;y<h;y++){
            for(int x=0;x<w;x++){
                int v = (int) Math.round(255.0 * (nms[y][x] / maxMag));
                int edge = (v >= high) ? 255 : (v >= low ? 128 : 0);
                int rgb = (edge<<16)|(edge<<8)|edge;
                out.setRGB(x,y,rgb);
            }
        }
        return out;
    }

    /**
     * Простой бокс-фильтр (усреднение) r×r; используется перед Canny.
     */
    private BufferedImage boxBlur(BufferedImage src, int r){
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w,h,BufferedImage.TYPE_BYTE_GRAY);
        int half = r/2;
        double norm = 1.0/(r*r);
        for(int y=0;y<h;y++){
            for(int x=0;x<w;x++){
                double sum = 0;
                for(int j=-half;j<=half;j++){
                    for(int i=-half;i<=half;i++){
                        int xx = Math.min(Math.max(x+i,0),w-1);
                        int yy = Math.min(Math.max(y+j,0),h-1);
                        sum += (src.getRGB(xx,yy) & 0xFF);
                    }
                }
                int v = clamp8((int) Math.round(sum*norm));
                int rgb = (v<<16)|(v<<8)|v;
                out.setRGB(x,y,rgb);
            }
        }
        return out;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ImageProcessingApp::new);
    }
}
