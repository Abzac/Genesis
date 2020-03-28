import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;


// Основной класс программы.
public class World extends JFrame {

    int width;
    int height;
    private int zoom;
    int seaLevel;
    private int drawStep;
    int[][] map;    //Карта мира
    private int[] mapInGPU;    //Карта для GPU
    private Image mapBuffer = null;
    Bot[][] matrix;    //Матрица мира
    private Bot zeroBot = new Bot();
    private Bot currentBot;
    private Bot currenGraphicstbot;
    int generation;
    private int population;
    private int organic;

    private Image buffer = null;

    private Thread thread = null;
    private Thread graphicsThread = null;
    private boolean running = true; // поток работает?
    private JPanel canvas = new JPanel() {
    	public void paint(Graphics g) {
    		g.drawImage(buffer, 0, 0, null);
    	}
    };

    private JPanel paintPanel = new JPanel(new FlowLayout());

    private JLabel generationLabel = new JLabel(" Generation: 0 ");
    private JLabel populationLabel = new JLabel(" Population: 0 ");
    private JLabel organicLabel = new JLabel(" Organic: 0 ");
    private JLabel coordsLabel = new JLabel(" Coords: 0,0 ");
    private JLabel colorLabel = new JLabel(" Color: 0, 0, 0 ");

//    private JLabel dnaLabel = new JLabel("");
    private JLabel memoryLabel = new JLabel("");

    private JSlider perlinSlider = new JSlider (JSlider.HORIZONTAL, 0, 480, 300);
    private JSlider zoomSlider = new JSlider (JSlider.HORIZONTAL, 0, 8, 1);
    private JButton mapButton = new JButton("Create Map");
    private JSlider sealevelSlider = new JSlider (JSlider.HORIZONTAL, 0, 256, 145);
    private JButton startButton = new JButton("Start/Stop");
    private JSlider drawstepSlider = new JSlider (JSlider.HORIZONTAL, 0, 40, 10);
    private JButton reStartButton = new JButton("RESTART");

    private JRadioButton baseButton = new JRadioButton("Base", true);
    private JRadioButton combinedButton = new JRadioButton("Combined", false);
    private JRadioButton energyButton = new JRadioButton("Energy", false);
    private JRadioButton mineralButton = new JRadioButton("Minerals", false);
    private JRadioButton ageButton = new JRadioButton("Age", false);
    private JRadioButton familyButton = new JRadioButton("Family", false);

    private DefaultListModel<String> commandsListModel = new DefaultListModel<>();
    private JList<String> commands_list = new JList<>(commandsListModel);
    private JScrollPane commandsListScroller = new JScrollPane(commands_list);

    boolean adamGenerated = false;
    Bot last_cursor_bot = null;

    public World() {
    	
        simulation = this;

        zoom = 1;
        seaLevel = 145;
        drawStep = 10;

        setTitle("Genesis 1.2.0-M");
        setSize(new Dimension(1800, 900));
        Dimension sSize = Toolkit.getDefaultToolkit().getScreenSize(), fSize = getSize();
        if (fSize.height > sSize.height) fSize.height = sSize.height;
        if (fSize.width  > sSize.width) fSize.width = sSize.width;
        //setLocation((sSize.width - fSize.width)/2, (sSize.height - fSize.height)/2);
        setSize(new Dimension(sSize.width, sSize.height));
        
        setDefaultCloseOperation (WindowConstants.EXIT_ON_CLOSE);

        Container container = getContentPane();

        paintPanel.setLayout(new BorderLayout());// у этого лейаута приятная особенность - центральная часть растягивается автоматически
        paintPanel.add(canvas, BorderLayout.CENTER);// добавляем нашу карту в центр
        container.add(paintPanel);

        JPanel statusPanel = new JPanel(new FlowLayout());
        statusPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        container.add(statusPanel, BorderLayout.SOUTH);

        generationLabel.setPreferredSize(new Dimension(140, 18));
        generationLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusPanel.add(generationLabel);
        populationLabel.setPreferredSize(new Dimension(140, 18));
        populationLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusPanel.add(populationLabel);
        organicLabel.setPreferredSize(new Dimension(140, 18));
        organicLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusPanel.add(organicLabel);
        coordsLabel.setPreferredSize(new Dimension(140, 18));
        coordsLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusPanel.add(coordsLabel);
        colorLabel.setPreferredSize(new Dimension(140, 18));
        colorLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusPanel.add(colorLabel);

        JPanel memoryPanel = new JPanel(new FlowLayout());
        memoryPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        memoryPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        container.add(memoryPanel, BorderLayout.NORTH);

//        dnaLabel.setBorder(BorderFactory.createLoweredBevelBorder());
//        dnaLabel.setPreferredSize(new Dimension(1200, 18));
//        memoryPanel.add(dnaLabel);

        memoryLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        memoryLabel.setPreferredSize(new Dimension(700, 18));
        memoryPanel.add(memoryLabel);


        JToolBar toolbar = new JToolBar();
        toolbar.setOrientation(1);
//        toolbar.setBorderPainted(true);
//        toolbar.setBorder(BorderFactory.createLoweredBevelBorder());
        container.add(toolbar, BorderLayout.WEST);

        JLabel slider1Label = new JLabel("Map scale");
        toolbar.add(slider1Label);

        perlinSlider.setMajorTickSpacing(160);
        perlinSlider.setMinorTickSpacing(80);
        perlinSlider.setPaintTicks(true);
        perlinSlider.setPaintLabels(true);
        perlinSlider.setPreferredSize(new Dimension(100, perlinSlider.getPreferredSize().height));
        perlinSlider.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        toolbar.add(perlinSlider);

        // JLabel sliderZoomLabel = new JLabel("Zoom");
        // toolbar.add(sliderZoomLabel);

        zoomSlider.addChangeListener(new zoomSliderChange());
        zoomSlider.setMajorTickSpacing(2);
        zoomSlider.setMinorTickSpacing(1);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPaintLabels(true);
        zoomSlider.setPreferredSize(new Dimension(100, zoomSlider.getPreferredSize().height));
        zoomSlider.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        // toolbar.add(zoomSlider);

        mapButton.addActionListener(new mapButtonAction());
        toolbar.add(mapButton);

        JLabel slider2Label = new JLabel("Sea level");
        toolbar.add(slider2Label);

        sealevelSlider.addChangeListener(new sealevelSliderChange());
        sealevelSlider.setMajorTickSpacing(128);
        sealevelSlider.setMinorTickSpacing(64);
        sealevelSlider.setPaintTicks(true);
        sealevelSlider.setPaintLabels(true);
        sealevelSlider.setPreferredSize(new Dimension(100, sealevelSlider.getPreferredSize().height));
        sealevelSlider.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        toolbar.add(sealevelSlider);

        startButton.addActionListener(new startButtonAction());
        toolbar.add(startButton);
        startButton.setEnabled(false);

        // JLabel clickAnywhereLabel = new JLabel("Draw step");
        // toolbar.add(clickAnywhereLabel);

        //drawstepSlider.addChangeListener(new drawstepSliderChange());
        drawstepSlider.setMajorTickSpacing(10);
//        drawstepSlider.setMinimum(1);
//        drawstepSlider.setMinorTickSpacing(64);
        drawstepSlider.setPaintTicks(true);
        drawstepSlider.setPaintLabels(true);
        drawstepSlider.setPreferredSize(new Dimension(100, sealevelSlider.getPreferredSize().height));
        drawstepSlider.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        //toolbar.add(drawstepSlider);

        ButtonGroup group = new ButtonGroup();
        group.add(baseButton);
        group.add(combinedButton);
        group.add(energyButton);
        group.add(mineralButton);
        group.add(ageButton);
        group.add(familyButton);
        toolbar.add(baseButton);
        toolbar.add(combinedButton);
        toolbar.add(energyButton);
        toolbar.add(mineralButton);
        toolbar.add(ageButton);
        toolbar.add(familyButton);

        JLabel clickAnywhereLabel = new JLabel("Click on the map to start");
        toolbar.add(clickAnywhereLabel);

        toolbar.add(reStartButton);
        reStartButton.setEnabled(false);
        reStartButton.addActionListener(new reStartButtonAction());

        baseButton.addActionListener(new changeViewTypeAction());
        combinedButton.addActionListener(new changeViewTypeAction());
        energyButton.addActionListener(new changeViewTypeAction());
        mineralButton.addActionListener(new changeViewTypeAction());
        ageButton.addActionListener(new changeViewTypeAction());
        familyButton.addActionListener(new changeViewTypeAction());

        JToolBar right_toolbar = new JToolBar();
        right_toolbar.setOrientation(1);
        container.add(right_toolbar, BorderLayout.EAST);

        JLabel commandsLabel = new JLabel("Last commands:");
        right_toolbar.add(commandsLabel);
        right_toolbar.add(commandsListScroller);
        commandsListScroller.setPreferredSize(new Dimension(200, height));

        canvas.addMouseMotionListener(new canvasMouseMoved());
        canvas.addMouseListener(new canvasMouse());

        this.pack();
        this.setVisible(true);
        setExtendedState(MAXIMIZED_BOTH);

        this.addWindowStateListener(new windowStateListener());
        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent componentEvent) {
                if (!adamGenerated) {
                    width = canvas.getWidth() / zoom;
                    height = canvas.getHeight() / zoom;
                    generateMap((int) (Math.random() * 10000));
                    currenGraphicstbot = null;
                    paintMapView();
                    paint1();
                }
            }
        });

    }


    class windowStateListener implements WindowStateListener {
        @Override
        public void windowStateChanged(WindowEvent e) {
            if (!adamGenerated) {
                width = canvas.getWidth() / zoom;
                height = canvas.getHeight() / zoom;
                generateMap((int) (Math.random() * 10000));
                currenGraphicstbot = null;
                paintMapView();
                paint1();
            }
        }
    }

    class canvasMouseMoved implements MouseMotionListener {
        @Override
        public void mouseDragged(MouseEvent e) {
            // e.getX(), e.getY()
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            Pair<Integer, Integer> origCoords = getOrigCoords(e);

            String s = " Coords: " + origCoords.v1 + "," + origCoords.v2 + " ";
            colorLabel.setForeground(new Color(127, 127,127));
            // last_cursor_bot = null;

            if (matrix != null) {
                try {
                    Bot bot = matrix[origCoords.v1][origCoords.v2];
                    if (bot != null && bot.isAlive()) {
                        s += " [!] ";

                        int c_red = bot.c_red % 256;
                        if (c_red < 0) c_red = 0;
                        int c_green = bot.c_green % 256;
                        if (c_green < 0) c_green = 0;
                        int c_blue = bot.c_blue % 256;
                        if (c_blue < 0) c_blue = 0;

                        colorLabel.setForeground(new Color(c_red, c_green, c_blue));

                        colorLabel.setText(" Color: " + bot.c_red + ", " + bot.c_green + ", " + bot.c_blue + " ");
                        printCommands(bot);
                        // dnaLabel.setText(makeString(bot.mind, "DNA: "));
                        String mem_text = makeString(bot.memory, "Bot memory: ");
                        mem_text += "; Health: " + bot.health + "; Mineral: " + bot.mineral + "; Age: " + bot.age + ";";
                        memoryLabel.setText(mem_text);
                        last_cursor_bot = bot;
                    }
                } catch (ArrayIndexOutOfBoundsException ee) {
                    coordsLabel.setText(s);
                    last_cursor_bot = null;
                    return;
                }
            }

            coordsLabel.setText(s);
        }

        private Pair<Integer, Integer> getOrigCoords(MouseEvent e) {
            Pair<Integer, Integer> scaleHint = getScaleHint(
                width, height, paintPanel.getWidth(), paintPanel.getHeight()
            );

            int origWidth = (int) (e.getX() * (width / (double) scaleHint.v1 ));
            int origHeight = (int) (e.getY() * (height / (double) scaleHint.v2 ));

            return new Pair<>(origWidth, origHeight);
        }

        private String makeString(byte[] data, String prefix) {
            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append(prefix);
            for (int i = 0; i < data.length - 1; i++) {
                String mem_block = String.valueOf(data[i]);
                if (data[i] < 10) {
                    mem_block = "0" + mem_block;
                }
                stringBuilder.append(mem_block);
                stringBuilder.append("\t");
            }

            if (data[data.length - 1] < 10) {
                stringBuilder.append("0");
            }
            stringBuilder.append(data[data.length - 1]);

            return stringBuilder.toString();
        }

        private void printCommands(Bot bot) {
            SimpleStack<CommandResult> last_actions = bot.last_actions.makeCopy();
            commandsListModel.clear();

            for (CommandResult command_result : last_actions) {

                String command_code;
                switch (command_result.command) {
                    case 0:  // мутация
                        command_code = "MUT";
                        break;
                    case 6:
                    case 1:  // чтение из памяти
                        command_code = "MEM_READ";
                        break;
                    case 10:
                    case 2:  // запись в память
                        command_code = "MEM_WRITE";
                        break;

                    case 11:
                    case 13:
                        command_code = "MEM_SET";
                        break;

                    case 14:
                        command_code = "JUMP";
                        break;

                    case 8:
                    case 3:  // увеличение ячейки памяти на 1
                        command_code = "MEM_INCR";
                        break;

                    case 9:
                    case 4:  // уменьшение ячейки памяти на 1
                        command_code = "MEM_DECR";
                        break;

                    case 7:
                    case 5:  // условный переход на основе памяти
                        command_code = "MEM_IF";
                        break;

                    case 53:
                    case 54:
                    case 55:
                    case 15:
                    case 16:  // размножение делением
                        command_code = "DOUBLE";
                        break;

                    case 19:
                    case 20:
                    case 56:
                    case 57:
                    case 58:
                    case 18:  // скрещивание
                        command_code = "CONV";
                        break;

                    case 21: // "поговорить"
                        command_code = "SAY";
                        break;

                    case 23:  // повернуть с параметром
                        command_code = "ROT";
                        break;
                    case 26:  // шаг с параметром
                        command_code = "MOVE";  // 2-пусто 3-стена 4-органика 5-бот 6-родня
                        break;

                    case 27:
                        command_code = "FIND_BOT";
                        break;
                    case 28:
                        command_code = "FIND_EMPTY";
                        break;
                    case 29:
                        command_code = "FIND_ORGANIC";
                        break;
                    case 30:
                        command_code = "FIND_RELATIVE";
                        break;
                    case 31:
                        command_code = "FIND_FOREIGN";
                        break;

                    case 32:  // фотосинтез
                        command_code = "PHOTO";
                        break;
                    case 33:  // хемосинтез (энерия из минералов)
                        command_code = "CHEMO";
                        break;
                    case 34:  // съесть в относительном напралении
                        command_code = "EAT";  // стена - 2 пусто - 3 органика - 4 живой - 5
                        break;
                    case 36:  // отдать безвозмездно в относительном напралении
                    case 37:  // стена - 2 пусто - 3 органика - 4 удачно - 5
                        command_code = "GIVE";
                        break;
                    case 38:  // распределить энергию в относительном напралении,
                    case 39:  // стена - 2 пусто - 3 органика - 4 удачно - 5
                        command_code = "CARE";
                        break;
                    case 40:  // посмотреть с параметром
                        command_code = "LOOK";  // пусто - 2 стена - 3 органик - 4 бот -5 родня -  6
                        break;
                    case 41:    // checkLevel() берет параметр из генома
                        command_code = "CHECK_LEVEL";  // возвращает 2, если рельеф выше, иначе - 3
                        break;
                    case 42:    // checkHealth() берет параметр из генома
                        command_code = "CHECK_HEALTH";  // возвращает 2, если здоровья больше, иначе - 3
                        break;
                    case 43:    // checkMineral() берет параметр из генома
                        command_code = "CHECK_MINERAL";  //  возвращает 2, если минералов больше, иначе - 3
                        break;
                    case 46:   // isFullAround()
                        command_code = "CHECK_AROUND";  // возвращает 1, если бот окружен и 2, если нет
                        break;
                    case 47:  // isHealthGrow() возвращает 1, если энегрия у бота прибавляется, иначе - 2
                        command_code = "CHECK_HP_GROW";
                        break;
                    case 48:   // isMineralGrow() возвращает 1, если энегрия у бота прибавляется, иначе - 2
                        command_code = "CHECK_MIN_GROW";
                        break;
                    case 52:  // бот атакует геном соседа, на которого он повернут
                        command_code = "GEN_ATTACK";
                        break;
                    default:
                        command_code = "NOP";
                        break;
                }

                String arguments = "()";
                if (command_result.arg1 != -1) {
                    if (command_result.arg2 != -1) {
                        arguments = "(" + command_result.arg1 + ", " + command_result.arg2 + ")";
                    } else {
                        arguments = "(" + command_result.arg1 + ")";
                    }
                }

                String result = ";";
                if (command_result.result != -1) {
                    result = " => " + command_result.result + ";";
                }

                String command = String.valueOf(command_result.command);
                if (command_result.command < 10) {
                    command = "0" + command;
                }

                commandsListModel.addElement(command + " " + command_code + arguments + result);
            }
        }
    }

    class canvasMouse implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!adamGenerated || zeroBot.next == zeroBot) {
                generateAdam(e.getX(), e.getY());
                adamGenerated = true;
                paint1();

                perlinSlider.setEnabled(false);
                mapButton.setEnabled(false);
                sealevelSlider.setEnabled(false);
                thread	= new Worker(); // создаем новый поток
                thread.start();
                graphicsThread = new GraphicsWorker();
                graphicsThread.start();
                startButton.setEnabled(true);
                reStartButton.setEnabled(true);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }

    class sealevelSliderChange implements ChangeListener {
        public void stateChanged(ChangeEvent event) {
            seaLevel = sealevelSlider.getValue();
            if (map != null) {
                paintMapView();
                paint1();
            }
        }
    }

    class zoomSliderChange implements ChangeListener {
        public void stateChanged(ChangeEvent event) {
            zoom = Math.max(1, zoomSlider.getValue());
        }
    }

    class mapButtonAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            width = canvas.getWidth() / zoom;    // Ширина доступной части экрана для рисования карты
            height = canvas.getHeight() / zoom;
            generateMap((int) (Math.random() * 10000));
            currenGraphicstbot = null;
            // generateAdam();
            paintMapView();
            paint1();
            adamGenerated = false;
        }
    }

    class changeViewTypeAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (!running) {
                paint1();
            }
        }
    }

    class startButtonAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
        	if(adamGenerated && thread==null && zeroBot.next != null) {
        	    perlinSlider.setEnabled(false);
        	    mapButton.setEnabled(false);
        	    sealevelSlider.setEnabled(false);
        		thread	= new Worker(); // создаем новый поток
        		thread.start();
        		graphicsThread = new GraphicsWorker();
                graphicsThread.start();
        	} else {
        		running = false;        //Выставляем влаг
        		thread = null;
                perlinSlider.setEnabled(true);
                sealevelSlider.setEnabled(true);
                mapButton.setEnabled(true);
        	}
        }
    }

    class reStartButtonAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            running = false;
            currenGraphicstbot = null;
            adamGenerated = false;
            zeroBot.next = zeroBot;
            zeroBot.prev = zeroBot;

            width = canvas.getWidth() / zoom;
            height = canvas.getHeight() / zoom;
            generation = 0;

            matrix = new Bot[width][height];

            startButton.setEnabled(false);
            sealevelSlider.setEnabled(false);

            paintMapView();
            paint1();
        }
    }

    public void paintMapView() {
        int mapred;
        int mapgreen;
        int mapblue;
        mapBuffer = canvas.createImage(width * zoom, height * zoom); // ширина - высота картинки
        Graphics g = mapBuffer.getGraphics();

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final int[] rgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < rgb.length; i++) {
            if (mapInGPU[i] < seaLevel) {                     // подводная часть
                mapred = 5;
                mapblue = 140 - (seaLevel - mapInGPU[i]) * 3;
                mapgreen = 150 - (seaLevel - mapInGPU[i]) * 10;
                if (mapgreen < 10) mapgreen = 10;
                if (mapblue < 20) mapblue = 20;
            } else {                                        // надводная часть
                mapred = (int)(150 + (mapInGPU[i] - seaLevel) * 2.5);
                mapgreen = (int)(100 + (mapInGPU[i] - seaLevel) * 2.6);
                mapblue = 50 + (mapInGPU[i] - seaLevel) * 3;
                if (mapred > 255) mapred = 255;
                if (mapgreen > 255) mapgreen = 255;
                if (mapblue > 255) mapblue = 255;
            }
            rgb[i] = (mapred << 16) | (mapgreen << 8) | mapblue;
        }
        g.drawImage(image, 0, 0, null);


    }

    public static BufferedImage scale(Image imageToScale, int dWidth, int dHeight) {
        BufferedImage scaledImage = null;
        if (imageToScale != null) {
            scaledImage = new BufferedImage(dWidth, dHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics2D = scaledImage.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.drawImage(imageToScale, 0, 0, dWidth, dHeight, null);
            graphics2D.dispose();
        }
        return scaledImage;
    }

    public static Pair<Integer, Integer> getScaleHint(int width, int height, int targetWidth, int targetHeight) {
        double originalRatio = (width / (double) height);
        double targetRatio = (targetWidth / (double) targetHeight);

        if (originalRatio > targetRatio) {
            targetHeight = (int) (targetWidth / originalRatio);
        } else {
            targetWidth = (int) (targetHeight * originalRatio);
        }

        return new Pair<>(targetWidth, targetHeight);
    }

    //    @Override
    public void paint1() {
        Bot cb = currenGraphicstbot;

        Image buf = canvas.createImage(width * zoom, height * zoom); //Создаем временный буфер для рисования
        Graphics g = buf.getGraphics(); //подменяем графику на временный буфер
        g.drawImage(mapBuffer, 0, 0, null);

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final int[] rgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        population = 0;
        organic = 0;
        int mapred, mapgreen, mapblue;

        while (cb != null && cb != zeroBot) {
            if (cb.isAlive() && !cb.deleted) {                      // живой бот
                if (baseButton.isSelected()) {
                    rgb[cb.y * width + cb.x] = (255 << 24) | (cb.c_red << 16) | (cb.c_green << 8) | cb.c_blue;
                } else if (energyButton.isSelected()) {
                    mapgreen = 255 - (int) (cb.health * 0.25);
                    if (mapgreen < 0) mapgreen = 0;
                    rgb[cb.y * width + cb.x] = (255 << 24) | (255 << 16) | (mapgreen << 8);
                } else if (mineralButton.isSelected()) {
                    mapblue = 255 - (int) (cb.mineral * 0.5);
                    if (mapblue < 0) mapblue = 0;
                    rgb[cb.y * width + cb.x] = (255 << 24) | (255 << 8) | mapblue;
                } else if (combinedButton.isSelected()) {
                    mapgreen = (int) (cb.c_green * (1 - cb.health * 0.0005));
                    if (mapgreen < 0) mapgreen = 0;
                    mapblue = (int) (cb.c_blue * (0.8 - cb.mineral * 0.0005));
                    rgb[cb.y * width + cb.x] = (255 << 24) | (cb.c_red << 16) | (mapgreen << 8) | mapblue;
                } else if (ageButton.isSelected()) {
                    mapred = 255 - (int) (Math.sqrt(cb.age) * 4);
                    if (mapred < 0) mapred = 0;
                    rgb[cb.y * width + cb.x] = (255 << 24) | (mapred << 16) | 255;
                } else if (familyButton.isSelected()) {
                    rgb[cb.y * width + cb.x] = cb.c_family;
                }
                population++;
            } else if (cb.alive == 1) {                                            // органика, известняк, коралловые рифы
                try {
                    if (map[cb.x][cb.y] < seaLevel) {                     // подводная часть
                        mapred = 20;
                        mapblue = 160 - (seaLevel - map[cb.x][cb.y]) * 2;
                        mapgreen = 170 - (seaLevel - map[cb.x][cb.y]) * 4;
                        if (mapblue < 40) mapblue = 40;
                        if (mapgreen < 20) mapgreen = 20;
                    } else {                                    // скелетики, трупики на суше
                        mapred = (int) (80 + (map[cb.x][cb.y] - seaLevel) * 2.5);   // надводная часть
                        mapgreen = (int) (60 + (map[cb.x][cb.y] - seaLevel) * 2.6);
                        mapblue = 30 + (map[cb.x][cb.y] - seaLevel) * 3;
                        if (mapred > 255) mapred = 255;
                        if (mapblue > 255) mapblue = 255;
                        if (mapgreen > 255) mapgreen = 255;
                    }
                    rgb[cb.y * width + cb.x] = (255 << 24) | (mapred << 16) | (mapgreen << 8) | mapblue;
                    organic++;
                } catch (ArrayIndexOutOfBoundsException err) {
                    break;
                }
            }
            cb = cb.next;
        }
        // cb = cb.next;

        g.drawImage(image, 0, 0, null);

        generationLabel.setText(" Generation: " + generation);
        populationLabel.setText(" Population: " + population);
        organicLabel.setText(" Organic: " + organic);

        int targetWidth = paintPanel.getWidth();
        int targetHeight = paintPanel.getHeight();
        Pair<Integer, Integer> scaleHint = getScaleHint(width, height, targetWidth, targetHeight);

        buf = scale(buf, scaleHint.v1, scaleHint.v2);

        buffer = buf;
        canvas.repaint();

//        // tried to fix the bug, when population does not want to start
//        if (population == 0) {
//            running = false;
//            thread = null;
//            perlinSlider.setEnabled(true);
//            sealevelSlider.setEnabled(true);
//            mapButton.setEnabled(true);
//            adamGenerated = false;
//        }
    }


    class Worker extends Thread {
        public void run() {
            running = true;         // Флаг работы потока, если false, поток заканчивает работу
            while (running) {       // обновляем матрицу
                while (currentBot != zeroBot) {
                    if (currentBot.isAlive()) {
                        currentBot.step();
                    }
                    currentBot = currentBot.next;
                }
                currentBot = currentBot.next;
                currenGraphicstbot = currentBot;
                generation++;
            }
            running = false;        // Закончили работу
        }
    }

    class GraphicsWorker extends Thread {
        public void run() {
            while (running) {
                // paintMapView();
                paint1();
            }
        }
    }

    public static World simulation;

    public static void main(String[] args) {
        simulation = new World();
    }


    // генерируем карту
    public void generateMap(int seed) {
        generation = 0;
        this.map = new int[width][height];
        this.matrix = new Bot[width][height];

        Perlin2D perlin = new Perlin2D(seed);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float f = (float) Math.max(1, perlinSlider.getValue());
                float value = perlin.getNoise(x/f,y/f,8,0.45f);        // вычисляем точку ландшафта
                map[x][y] = (int)(value * 255 + 128) & 255;
            }
        }
        mapInGPU = new int[width * height];
        for (int i=0; i<width; i++) {
            for(int j=0; j<height; j++) {
                mapInGPU[j*width+i] = map[i][j];
            }
        }
    }

    // генерируем первого бота
    public void generateAdam(int x, int y) {

        Bot bot = new Bot();
        zeroBot.prev = bot;
        zeroBot.next = bot;

        bot.adr = 0;            // начальный адрес генома
        bot.x = x;      // координаты бота
        bot.y = y;
        bot.health = 990;       // энергия
        bot.mineral = 0;        // минералы
        bot.alive = 3;          // бот живой
        bot.age = 0;            // возраст
        bot.c_red = 170;        // задаем цвет бота
        bot.c_blue = 170;
        bot.c_green = 170;
        bot.direction = 5;      // направление
        bot.prev = zeroBot;     // ссылка на предыдущего
        bot.next = zeroBot;     // ссылка на следующего
        for (int i = 0; i < Bot.MIND_SIZE; i++) {          // заполняем геном командой 32 - фотосинтез
            bot.mind[i] = 32;
        }
        for (int i = 0; i < Bot.MEMORY_SIZE; i++) {          // заполняем геном командой 32 - фотосинтез
            bot.memory[i] = 8;
        }

        matrix[bot.x][bot.y] = bot;             // помещаем бота в матрицу
        currentBot = bot;                       // устанавливаем текущим
        currenGraphicstbot = bot;
    }


}
