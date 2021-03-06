public class Bot {

    private static final int LV_ORGANIC_HOLD = 1;           // органика
    private static final int LV_ALIVE = 3;                  // живой бот
    public static final int MIND_SIZE = 64;                // объем памяти генома
    public static final int MEMORY_SIZE = 4;                // объем памяти бота
    private static final int MEMORY_STATE_SIZE = 16;                // количество состояний ячейки памяти бота

    private static double[] randMemory = new double[10000000];   // массив предгенерированных случайных чисел
    private static int randIdx = 0;                             // указатель текущего случайного числа
    static {                                                    // предгенерация массива случайных чисел
        for (int i = 0; i < randMemory.length; i++) {
            randMemory[i] = Math.random();
        }
    }


    byte[] mind = new byte[MIND_SIZE];               // геном бота
    byte[] memory = new byte[MEMORY_SIZE];          // память бота
    SimpleStack<CommandResult> last_actions = new SimpleStack<>(100); // послдение действия бота

    int adr;                 // указатель текущей команды
    int x;                   // координаты
    int y;
    int direction;           // нправление
    int alive;               // бот живой - 3
    int health;              // энергия
    int mineral;             // минералы
    int age;                 // возраст

    int c_red;               // цвет бота базовый, зависит от питания
    int c_green;
    int c_blue;
    int c_family;            // цвет семьи бота в ARGB
    boolean deleted = false;

    Bot prev;                // предыдущий в цепочке просчета
    Bot next;                // следующий в цепочке просчета

    public boolean isAlive() {
        return alive == LV_ALIVE;
    }

    // ====================================================================
    // =========== главная функция жизнедеятельности бота  ================
    // =========== в ней выполняется код его мозга-генома  ================
    void step() {
        int breakflag;
        int command;

        // memory variables
        int action;
        int memory_address;
        byte value;

        for (int cyc = 0; cyc < 15; cyc++) {
            command = mind[adr];        // текущая команда
            CommandResult command_result = new CommandResult(command, adr);
            last_actions.add(command_result);

            breakflag = 0;
            switch (command) {

//*******************************************************************
//................      мутировать   ................................
                case 0:
                    botMutate();
                    botIncAdr(1);   // смещаем указатель текущей команды на 1
                    breakflag = 1;     // выходим, так как команда завершающая
                    break;

                case 6:
                case 1: // читать память
                    memory_address = botGetParam() % MEMORY_SIZE;
                    command_result.arg1 = memory_address;
                    botJumpAdr(memory[memory_address] + 1);
                    break;

                case 10:
                case 2: // писать память
                    memory_address = botGetParam() % MEMORY_SIZE;
                    command_result.arg1 = memory_address;
                    action = botGetParam(1);
                    command_result.arg2 = (action % 11) + 1;
                    value = (byte) (getActionValue(action) % MEMORY_STATE_SIZE);
                    command_result.result = value;

                    memory[memory_address] = value;
                    botIncAdr(3);
                    breakflag = 1;
                    break;

                case 11:
                case 13:  // установить память
                    memory_address = botGetParam() % MEMORY_SIZE;
                    command_result.arg1 = memory_address;
                    value = (byte) (botGetParam(1) % MEMORY_STATE_SIZE);
                    command_result.arg2 = value;
                    memory[memory_address] = value;
                    botIncAdr(3);
                    break;

                case 8:
                case 3: // увеличить память
                    memory_address = botGetParam() % MEMORY_SIZE;
                    command_result.arg1 = memory_address;
                    memory[memory_address] = (byte) ((memory[memory_address] + 1) % MEMORY_STATE_SIZE);
                    command_result.result = memory[memory_address];
                    botIncAdr(2);
                    break;

                case 9:
                case 4: // уменьшить память
                    memory_address = botGetParam() % MEMORY_SIZE;
                    command_result.arg1 = memory_address;
                    memory[memory_address] = (byte) (Math.abs(memory[memory_address] - 1) % MEMORY_STATE_SIZE);
                    command_result.result = memory[memory_address];
                    botIncAdr(2);
                    break;

                case 7:
                case 5: // условный переход
                    memory_address = botGetParam() % MEMORY_SIZE;
                    command_result.arg1 = memory_address;
                    int memory_threshold = botGetParam(1) % MEMORY_STATE_SIZE;
                    command_result.arg2 = memory_threshold;

                    if (memory[memory_address] >= memory_threshold) {
                        botJumpAdr(memory_threshold + 3);
                        command_result.result = 1;
                    } else {
                        botJumpAdr(memory_threshold + 4);
                        command_result.result = 0;
                    }

                    break;


                case 14:  // unconditional jump
                    command_result.arg1 = botGetParam();
                    botJumpAdr(command_result.arg1 + 1);
                    command_result.result = adr;
                    break;

//*******************************************************************
//............... размножение делением ..............................
                case 15:
                case 16:
                case 53:
                case 54:
                case 55:
                    command_result.result = botDouble();
                    botIncAdr(1);
                    breakflag = 1;
                    break;

                case 19:
                case 20:
                case 56:
                case 57:
                case 58:
                case 18: // размножение половое
                    command_result.result = botConvergence();
                    botIncAdr(1);
                    breakflag = 1;
                    break;

                case 21:  // bot voice interactions
                    command_result.result = sayBot();
                    botIncAdr(1);
                    breakflag = 1;
                    break;


//*******************************************************************
//...............  повернуть с параметром   .........................
                case 23:
                    command_result.arg1 = botGetParam() % 8;
                    command_result.result = botRotate();
                    botIncAdr(2);
                    break;
//*******************************************************************
//...............  шаг с параметром  ................................
                case 26:
                    command_result.arg1 = botGetParam() % 8;
                    command_result.result = botMove();
                    botJumpAdr(command_result.result); // смещаем УТК на значение клетки (botMove(): 2-пусто  3-стена  4-органика 5-бот 6-родня)
                    breakflag = 1;
                    break;

                case 27:  // 27-31: commands for bots to find different things
                    command_result.result = findBot();
                    if (command_result.result != 8) direction = command_result.result;
                    botIncAdr(1);
                    breakflag = 1;
                    break;
                case 28:
                    command_result.result = findEmptyDirection();
                    if (command_result.result != 8) direction = command_result.result;
                    botIncAdr(1);
                    breakflag = 1;
                    break;
                case 29:
                    command_result.result = findOrganic();
                    if (command_result.result != 8) direction = command_result.result;
                    botIncAdr(1);
                    breakflag = 1;
                    break;
                case 30:
                    command_result.result = findRelativeBot();
                    if (command_result.result != 8) direction = command_result.result;
                    botIncAdr(1);
                    breakflag = 1;
                    break;
                case 31:
                    command_result.result = findForeignBot();
                    if (command_result.result != 8) direction = command_result.result;
                    botIncAdr(1);
                    breakflag = 1;
                    break;

//*******************************************************************
//...............  фотосинтез .......................................
                case 32:
                    command_result.result = botEatSun();
                    botIncAdr(1);
                    breakflag = 1;
                    break;
//*******************************************************************
//............... хемосинтез (энерия из минералов) ..................
                case 33:
                    command_result.result = botMineral2Health();
                    botIncAdr(1);
                    breakflag = 1;
                    break;
//************************************************************************
//..............   съесть в относительном напралении       ...............
                case 34:
                    command_result.arg1 = botGetParam() % 8;
                    command_result.result = botEatBot();
                    botJumpAdr(command_result.result); // меняем адрес текущей команды
                    // стена - 2 пусто - 3 органика - 4 живой - 5
                    breakflag = 1;
                    break;


//************************************************************************
//.............   отдать безвозмездно в относительном напралении  ........
                case 36:
                case 37:    // увеличил шансы появления этой команды
                    command_result.arg1 = botGetParam() % 8;
                    command_result.result = botGive();
                    botJumpAdr(command_result.result); // меняем адрес текущей команды
                    // стена - 2 пусто - 3 органика - 4 удачно - 5
                    break;
//************************************************************************
//.............   распределить энергию в относительном напралении  .......
                case 38:
                case 39:    // увеличил шансы появления этой команды
                    command_result.arg1 = botGetParam() % 8;
                    command_result.result = botCare();
                    botJumpAdr(command_result.result); // меняем адрес текущей команды
                    // стена - 2 пусто - 3 органика - 4 удачно - 5
                    break;


//************************************************************************
//.............   посмотреть с параметром ................................
                case 40:
                    command_result.arg1 = botGetParam() % 8;
                    command_result.result = botSeeBots();
                    botJumpAdr(command_result.result); // меняем адрес текущей команды
                    // пусто - 2 стена - 3 органик - 4 бот -5 родня -  6
                    break;


//***********************************************************************
//...................  проверка уровня рельефа  .........................
                case 41:    // checkLevel() берет параметр из генома, возвращает 2, если рельеф выше, иначе - 3
                    command_result.arg1 = botGetParam() % 8;
                    command_result.result = checkLevel();
                    botJumpAdr(command_result.result);
                    break;
//***********************************************************************
//...................  проверка здоровья  ...............................
                case 42:    // checkHealth() берет параметр из генома, возвращает 2, если здоровья больше, иначе - 3
                    command_result.arg1 = botGetParam() % 8;
                    command_result.result = checkHealth();
                    botJumpAdr(command_result.result);
                    break;
//***********************************************************************
//...................  проверка  минералов ..............................
                case 43:    // checkMineral() берет параметр из генома, возвращает 2, если минералов больше, иначе - 3
                    command_result.arg1 = botGetParam() % 8;
                    command_result.result = checkMineral();
                    botJumpAdr(command_result.result);
                    break;

//*************************************************************
//...............  окружен ли бот?   ..........................
                case 46:   // isFullAround() возвращает  1, если бот окружен и 2, если нет
                    command_result.result = isFullAround();
                    botJumpAdr(command_result.result);
                    break;
//*************************************************************
//.............. приход энергии есть? .........................
                case 47:  // isHealthGrow() возвращает 1, если энегрия у бота прибавляется, иначе - 2
                    command_result.result = isHealthGrow();
                    botJumpAdr(command_result.result);
                    break;
//*************************************************************
//............... минералы прибавляются? ......................
                case 48:   // isMineralGrow() возвращает 1, если энегрия у бота прибавляется, иначе - 2
                    command_result.result = isMineralGrow();
                    botJumpAdr(command_result.result);
                    break;


//********************************************************************
//................   генная атака  ...................................
                case 52:  // бот атакует геном соседа, на которого он повернут
                    command_result.result = botGenAttack(); // случайным образом меняет один байт
                    botIncAdr(1);
                    breakflag = 1;
                    break;

//=======================================================================
//................    если ни с одной команд не совпало .................
//................    значит безусловный переход        .................
//.....   прибавляем к указателю текущей команды значение команды   .....
                default:
                    botIncAdr(command);
                    break;
            }
            if (breakflag == 1) break;
        }

//###########################################################################
//.......  выход из функции и передача управления следующему боту   ........
//.......  но перед выходом нужно проверить                         ........
//.......  количество накопленой энергии, возможно                  ........
//.......  пришло время подохнуть или породить потомка              ........

        if (isAlive()) {

            //... проверим уровень энергии у бота, возможно пришла пора помереть или родить
            if (health > 999) {
                if (rand() > 0.25) {
                    if (findRelativeBot() != 8) {
                        botConvergence();
                    } else {
                        botDouble();
                    }
                } else {
                    health = 750;
                }
            }

            health -= 3;                            // каждый ход отнимает 3 единицы энегрии
            age++;                                  // увеличиваем возраст
            if (health <= 0) {                      // если энергии стало меньше 1
                bot2Organic();                      // то время умирать, превращаясь в огранику
                return;                             // и передаем управление к следующему боту
            }

            if (age >= 10000) {                      // если возраст больше
                if (rand() < 0.001) {
                    bot2Organic();                  // то время умирать, превращаясь в огранику
                    return;                         // и передаем управление к следующему боту
                }
            }

            if (rand() < 0.0001) {
                bot2Organic();
                return;
            }

            int level = World.simulation.map[x][y];
            int sealevel = World.simulation.seaLevel;
            if ((level > sealevel - 30) && (level <= sealevel)) {
                if (level <= sealevel - 20) {
                    mineral++;
                } else if (level <= sealevel - 10) {
                    mineral += 2;
                } else {
                    mineral += 3;
                }
            }
            if (mineral > 1000) mineral = 1000;
        }
    }


    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    // -- получение Х-координаты рядом        ---------
    //  с био по относительному направлению  ----------
    // out - X -  координата             --------------
    private int xFromVektorR(int n) {
        int xt = x;
        n = n + direction;
        if (n >= 8) n = n - 8;
        if (n == 0 || n == 6 || n == 7) {
            xt--;
            if (xt < 0) xt = World.simulation.width - 1;
        } else if (n >= 2 && n <= 4) {
            xt++;
            if (xt >= World.simulation.width) xt = 0;
        }
        return xt;
    }

    //жжжжжжжжжжжжхжжжжжхжжжжжжхжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    // ------ получение Y-координаты рядом              ---------
    // ---- Y координата по относительному направлению  ----------
    // ---  out - Y -  координата                    -------------
    private int yFromVektorR(int n) {
        int yt = y;
        n = n + direction;
        if (n >= 8) n = n - 8;
        if (n <= 2) {
            yt--;
        } else if (n >= 4 && n <= 6) {
            yt++;
        }
        return yt;
    }

    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    //===========   окружен ли бот          ==========
    //===== out  1-окружен  2-нет           ===
    private int isFullAround() {
        int xt, yt;
        if ((y > 0) && (y < World.simulation.height-1) && (x > 0) && (x < World.simulation.width-1)) {
            if (World.simulation.matrix[x-1][y-1] == null) return 2;    // это все ради оптимизации, я плакал когда это писал(((
            if (World.simulation.matrix[x+1][y+1] == null) return 2;
            if (World.simulation.matrix[x-1][y+1] == null) return 2;
            if (World.simulation.matrix[x+1][y-1] == null) return 2;
            if (World.simulation.matrix[x][y-1] == null) return 2;
            if (World.simulation.matrix[x][y+1] == null) return 2;
            if (World.simulation.matrix[x-1][y] == null) return 2;
            if (World.simulation.matrix[x+1][y] == null) return 2;
        } else {
            for (int i = 0; i < 8; i++) {
                xt = xFromVektorR(i);
                yt = yFromVektorR(i);
                if ((yt >= 0) && (yt < World.simulation.height)) {
                    if (World.simulation.matrix[xt][yt] == null) return 2;
                }
            }
        }
        return 1;
    }


    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    //==== ищет свободные ячейки вокруг бота ============
    //==== начинает спереди и дальше по      ============
    //==== кругу через низ    ( world )      ============
    //==== out - номер направление или       ============
    //====  или 8 , если свободных нет       ============
    private int findEmptyDirection() {
        int xt, yt;
        for (int i = 0; i < 8; i++) {
            xt = xFromVektorR(i);
            yt = yFromVektorR(i);
            if ((yt >= 0) && (yt < World.simulation.height)) {
                if (World.simulation.matrix[xt][yt] == null) return i;
            }
        }
        return 8;       // свободных нет
    }

    private Bot getBotByDirection(int n) {
        if (n == 8) {
            return null;
        }
        int xt = xFromVektorR(n);
        int yt = yFromVektorR(n);
        return World.simulation.matrix[xt][yt];
    }

    private int findBot() {
        int xt, yt;
        for (int i = 0; i < 8; i++) {
            xt = xFromVektorR(i);
            yt = yFromVektorR(i);
            if ((yt >= 0) && (yt < World.simulation.height)) {
                Bot bot = World.simulation.matrix[xt][yt];
                if (bot != null && bot.isAlive()) return i;
            }
        }
        return 8;       // свободных нет
    }

    private int findRelativeBot() {
        int xt, yt;
        for (int i = 0; i < 8; i++) {
            xt = xFromVektorR(i);
            yt = yFromVektorR(i);
            if ((yt >= 0) && (yt < World.simulation.height)) {
                Bot bot = World.simulation.matrix[xt][yt];
                if (bot != null && isRelative(bot)) return i;
            }
        }
        return 8;       // свободных нет
    }

    private int findForeignBot() {
        int xt, yt;
        for (int i = 0; i < 8; i++) {
            xt = xFromVektorR(i);
            yt = yFromVektorR(i);
            if ((yt >= 0) && (yt < World.simulation.height)) {
                Bot bot = World.simulation.matrix[xt][yt];
                if (bot != null && !isRelative(bot)) return i;
            }
        }
        return 8;       // свободных нет
    }

    private int findOrganic() {
        int xt, yt;
        for (int i = 0; i < 8; i++) {
            xt = xFromVektorR(i);
            yt = yFromVektorR(i);
            if ((yt >= 0) && (yt < World.simulation.height)) {
                Bot bot = World.simulation.matrix[xt][yt];
                if (bot != null && (bot.alive == LV_ORGANIC_HOLD)) return i;
            }
        }
        return 8;       // свободных нет
    }

    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    // -- получение параметра для команды   --------------
    // out - возвращает число из днк, следующее за выполняемой командой
    private int botGetParam() {
        return mind[(adr + 1) % MIND_SIZE]; // возвращает число, следующее за выполняемой командой
    }

    private int botGetParam(int a) {
        return mind[(adr + a + 1) % MIND_SIZE]; // возвращает число, следующее за выполняемой командой
    }

    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    // -- увеличение адреса команды   --------------
    private void botIncAdr(int a) {
        adr = (adr + a) % MIND_SIZE;
    }

    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    //---- косвенное увеличение адреса команды   --------------
    private void botJumpAdr(int a) {
        int bias = mind[(adr + a) % MIND_SIZE];
        botIncAdr(bias);
    }

    private int getActionValue(int action) {
        int value;

        action = (action % 11) + 1;
        switch (action) {
            case 1:
                value = botMove();
                break;
            case 2:
                value = botEatBot();
                break;
            case 3:
                value = botGive();
                break;
            case 4:
                value = botCare();
                break;
            case 5:
                value = botSeeBots();
                break;
            case 6:
                value = checkLevel();
                break;
            case 7:
                value = checkHealth();
                break;
            case 8:
                value = checkMineral();
                break;
            case 9:
                value = isFullAround();
                break;
            case 10:
                value = isHealthGrow();
                break;
            case 11:
                value = isMineralGrow();
                break;
            default:
                value = 0;
        }

        return value;
    }


    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    //=====  превращение бота в органику    ===========
    private void bot2Organic() {
        alive = LV_ORGANIC_HOLD;       // отметим в массиве bots[], что бот органика
    }


    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    //===== изменяет случайный байт в геноме  ==============
    private void botMutate() {
        int ma = (int) (rand() * MIND_SIZE);    // 0..63 // меняются случайным образом две случайные команды
        int mc = (int) (rand() * MIND_SIZE);    // 0..63
        mind[ma] = (byte) mc;
        ma = (int) (rand() * MIND_SIZE);        // 0..63
        mc = (int) (rand() * MIND_SIZE);        // 0..63
        mind[ma] = (byte) mc;
    }


    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    //===== поворачивает бота в заданном параметром направлении  ==============
    private int botRotate() {
        direction = (direction + botGetParam()) % 8;
        return direction;
    }

    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    //===== перемещает бота в нужную точку  ==============
    //===== без проверок                    ==============
    //===== in - номер бота и новые координаты ===========
    private void moveBot(int xt, int yt) {
        World.simulation.matrix[xt][yt] = this;
        World.simulation.matrix[x][y] = null;
        x = xt;
        y = yt;
    }

    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    //=====   удаление бота        =============
    private void deleteBot(Bot bot) {
        bot.deleted = true;
        World.simulation.matrix[bot.x][bot.y] = null;   // удаление бота с карты

        bot.prev.next = bot.next;            // удаление бота из цепочки
        bot.next.prev = bot.prev;            // связывающей всех ботов
    }


    //=========================================================================================
    //============================       КОД КОМАНД   =========================================
    //=========================================================================================
    // ...  фотосинтез, этой командой забит геном первого бота     ...............
    // ...  бот получает энергию солнца в зависимости от глубины   ...............
    // ...  и количества минералов, накопленных ботом              ...............
    private int botEatSun() {
        int t;
        if (mineral < 100) {
            t = 0;
        } else if (mineral < 400) {
            t = 1;
        } else {
            t = 2;
        }

        int hlt = 0;
        int level = World.simulation.map[x][y];
        int sealevel = World.simulation.seaLevel;
        if ((level > sealevel) && (level <= sealevel + 60)) {
            hlt = t + (int) ((sealevel + 60 - level) * 0.2); // формула вычисления энергии
        }
        if (hlt > 0) {
            health = health + hlt;          // прибавляем полученную энергия к энергии бота
            goGreen(hlt);                   // бот от этого зеленеет
            return hlt;
        }
        return 0;
    }


    // ...  преобразование минералов в энергию  ...............
    private int botMineral2Health() {
        if (mineral > 100) {                // максимальное количество минералов, которые можно преобразовать в энергию = 100
            mineral -= 100;
            health += 400;                  // 1 минерал = 4 энергии
            goBlue( 100);             // бот от этого синеет
            return 400;
        } else {                            // если минералов меньше 100, то все минералы переходят в энергию
            goBlue(mineral);
            health = health + 4 * mineral;
            mineral = 0;
            return 4 * mineral;
        }
    }

    //===========================  перемещение бота   ========================================
    private int botMove() { // ссылка на бота, направлелие и флажок(относительное или абсолютное направление)
        // на выходе   2-пусто  3-стена  4-органика 5-бот 6-родня
        int direction = botGetParam() % 8;
        int xt = xFromVektorR(direction);
        int yt = yFromVektorR(direction);

        if ((yt < 0) || (yt >= World.simulation.height)) {  // если там ... стена
            return 3;                       // то возвращаем 3
        }
        if (World.simulation.matrix[xt][yt] == null) {  // если клетка была пустая,
            moveBot(xt, yt);    // то перемещаем бота
            return 2;                       // и функция возвращает 2
        }
        // осталось 2 варианта: ограника или бот
        if (World.simulation.matrix[xt][yt].alive == LV_ORGANIC_HOLD) { // если на клетке находится органика
            return 4;                       // то возвращаем 4
        }
        if (isRelative(World.simulation.matrix[xt][yt])) {  // если на клетке родня
            return 6;                       // то возвращаем 6
        }
        return 5;                           // остался только один вариант - на клетке какой-то бот возвращаем 5
    }

    //============================    скушать другого бота или органику  ==========================================
    private int botEatBot() { // на входе ссылка на бота, направлелие и флажок(относительное или абсолютное направление)
        // на выходе пусто - 2  стена - 3  органик - 4  бот - 5
        int direction = botGetParam() % 8;

        health = health - 4;                // бот теряет на этом 4 энергии в независимости от результата
        int xt;
        int yt;

        xt = xFromVektorR(direction);       // вычисляем координату клетки, с которой хочет скушать бот (относительное направление)
        yt = yFromVektorR(direction);

        if ((yt < 0) || (yt >= World.simulation.height)) {  // если там стена возвращаем 3
            return 3;
        }
        if (World.simulation.matrix[xt][yt] == null) {  // если клетка пустая возвращаем 2
            return 2;
        }
        // осталось 2 варианта: органика или бот
        else if (World.simulation.matrix[xt][yt].alive == LV_ORGANIC_HOLD) {   // если там оказалась органика
            deleteBot(World.simulation.matrix[xt][yt]);                        // то удаляем её из списков
            health = health + 100;          //здоровье увеличилось на 100
            goRed(100);               // бот покраснел
            return 4;                       // возвращаем 4
        }
        //--------- дошли до сюда, значит впереди живой бот -------------------
        int min0 = mineral;                 // определим количество минералов у бота
        int min1 = World.simulation.matrix[xt][yt].mineral;  // определим количество минералов у потенциального обеда
        int hl = World.simulation.matrix[xt][yt].health;  // определим энергию у потенциального обеда
        // если у бота минералов больше
        if (min0 >= min1) {
            mineral = min0 - min1;          // количество минералов у бота уменьшается на количество минералов у жертвы
            // типа, стесал свои зубы о панцирь жертвы
            deleteBot(World.simulation.matrix[xt][yt]);          // удаляем жертву из списков
            int cl = 100 + (hl / 2); // Math.min(hl + 100, 200 + (hl / 2));        // количество энергии у бота прибавляется на 100+(половина от энергии жертвы)
            health = health + cl;
            goRed(cl);                      // бот краснеет
            return 5;                       // возвращаем 5
        }
        //если у жертвы минералов больше ----------------------
        mineral = 0;                        // то бот израсходовал все свои минералы на преодоление защиты
        min1 = min1 - min0;                 // у жертвы количество минералов тоже уменьшилось
        World.simulation.matrix[xt][yt].mineral = min1;       // перезаписали минералы жертве =========================ЗАПЛАТКА!!!!!!!!!!!!
        //------ если здоровья в 2 раза больше, чем минералов у жертвы  ------
        //------ то здоровьем проламываем минералы ---------------------------
        if (health >= 2 * min1) {
            deleteBot(World.simulation.matrix[xt][yt]);         // удаляем жертву из списков
            int cl = 100 + (hl / 2) - 2 * min1; // вычисляем, сколько энергии смог получить бот
            health = health + cl;
            if (cl < 0)
                cl = 0; //========================================================================================ЗАПЛАТКА!!!!!!!!!!! - энергия не должна быть отрицательной
            goRed(cl);                      // бот краснеет
            return 5;                       // возвращаем 5
        }
        //--- если здоровья меньше, чем (минералов у жертвы)*2, то бот погибает от жертвы
        World.simulation.matrix[xt][yt].mineral = min1 - (health / 2);  // у жертвы минералы истраченны
        health = 0;                         // здоровье уходит в ноль
        return 5;                           // возвращаем 5
    }

    //======================  посмотреть ==================================================
    private int botSeeBots() { // на входе ссылка на бота, направлелие и флажок(относительное или абсолютное направление)
        // на выходе  пусто - 2  стена - 3  органик - 4  бот - 5  родня - 6
        int direction = botGetParam() % 8;
        int xt = xFromVektorR(direction);                   // выясняем, есть ли что в этом  направлении (относительном)
        int yt = yFromVektorR(direction);

        if (yt < 0 || yt >= World.simulation.height) {              // если там стена возвращаем 3
            return 3;
        } else if (World.simulation.matrix[xt][yt] == null) {       // если клетка пустая возвращаем 2
            return 2;
        } else if (World.simulation.matrix[xt][yt].alive == LV_ORGANIC_HOLD) { // если органика возвращаем 4
            return 4;
        } else if (isRelative(World.simulation.matrix[xt][yt])) {   // если родня, то возвращаем 6
            return 6;
        } else {                                                    // если какой-то бот, то возвращаем 5
            return 5;
        }
    }

    //======================  провеа уровня рельефа ==========================================
    private int checkLevel() {
        if (World.simulation.map[x][y] < 256 * botGetParam() / MIND_SIZE) return 2;
        return 3;
    }

    //======================  провеа здоровья ================================================
    private int checkHealth() {
        if (health < 1000 * botGetParam() / MIND_SIZE) return 2;
        return 3;
    }

    //======================  провеа минералов ===============================================
    private int checkMineral() {
        if (mineral < 1000 * botGetParam() / MIND_SIZE) return 2;
        return 3;
    }



    //======== атака на геном соседа, меняем случайны ген случайным образом  ===============
    private int botGenAttack() {   // вычисляем кто перед ботом (используется только относительное направление)
        int xt = xFromVektorR(0);
        int yt = yFromVektorR(0);
        if ((yt >= 0) && (yt < World.simulation.height) && (World.simulation.matrix[xt][yt] != null)) {
            if (World.simulation.matrix[xt][yt].isAlive()) { // если там живой бот
                health = health - 10;               // то атакуюий бот теряет на атаку 10 энергии
                if (health > 0) {                   // если он при этом не умер
                    int ma = (int) (rand() * MIND_SIZE);   // 0..63 // то у жертвы случайным образом меняется один ген
                    int mc = (int) (rand() * MIND_SIZE);   // 0..63
                    World.simulation.matrix[xt][yt].mind[ma] = (byte) mc;
                    return 2;
                }
                return 1;
            }
        }
        return 0;
    }

    private Pair<Bot, Integer> botOp() {
        // на выходе стена - 2 пусто - 3 органика - 4 удачно - 5
        int direction = botGetParam() % 8;
        int xt = xFromVektorR(direction);       // определяем координаты для относительного направления
        int yt = yFromVektorR(direction);

        if (yt < 0 || yt >= World.simulation.height) {  // если там стена возвращаем 3
            return new Pair<>(null, 3);
        } else if (World.simulation.matrix[xt][yt] == null) {  // если клетка пустая возвращаем 2
            return new Pair<>(null, 2);
        } else if (World.simulation.matrix[xt][yt].alive == LV_ORGANIC_HOLD) { // если органика возвращаем 4
            return new Pair<>(null, 4);
        }
        return new Pair<>(World.simulation.matrix[xt][yt], 5);
    }

    //==========               поделиться          ====================================================
    // =========  если у бота больше энергии или минералов, чем у соседа в заданном направлении  =====
    //==========  то бот делится излишками                                                       =====
    private int botCare() { // на входе ссылка на бота, направлелие и флажок(относительное или абсолютное направление)
        // на выходе стена - 2 пусто - 3 органика - 4 удачно - 5
        Pair<Bot, Integer> result = botOp();
        if (result.v2 != 5) {
            return result.v2;
        }

        //------- если мы здесь, то в данном направлении живой бот ----------
        Bot other = result.v1;
        if (this.health > other.health) {                  // если у бота больше энергии, чем у соседа
            int hlt = (this.health - other.health) / 2;    // то распределяем энергию поровну
            this.health -= hlt;
            other.health += hlt;
        }
        if (this.mineral > other.mineral) {                  // если у бота больше минералов, чем у соседа
            int min = (this.mineral - other.mineral) / 2;    // то распределяем их поровну
            this.mineral -= min;
            other.mineral += min;
            if (other.mineral > 999) {
                other.mineral = 999;
            }
        }
        return result.v2;
    }


    //=================  отдать безвозместно, то есть даром    ==========
    private int botGive() {// на входе ссылка на бота, направлелие и флажок(относительное или абсолютное направление)
        // на выходе стена - 2 пусто - 3 органика - 4 удачно - 5
        Pair<Bot, Integer> result = botOp();
        if (result.v2 != 5) {
            return result.v2;
        }

        //------- если мы здесь, то в данном направлении живой ----------
        Bot other = result.v1;

        int hlt = this.health / 4;      // бот отдает четверть своей энергии
        this.health -= hlt;
        other.health += hlt;

        if (this.mineral > 3) {                 // только если их у него не меньше 4
            int min = this.mineral / 4;  // бот отдает четверть своих минералов
            this.mineral -= min;
            other.mineral += min;
            if (other.mineral > 999) {
               other.mineral = 999;
            }
        }
        return result.v2;
    }

    private int sayBot() {
        int rel_dir = findRelativeBot();
        if (rel_dir != 8) {
            Bot other = getBotByDirection(rel_dir);
            if (other != null) {
                System.arraycopy(this.mind, 0, other.mind, 0, 4);
                return 1;
            }
        }
        return 0;
    }

    private int botConvergence() {
        this.health -= 150;
        if (health <= 0) return 0;

        int n = findEmptyDirection();
        if (n == 8) {
            return 0;
        }

//        Pair<Bot, Integer> result = botOp();
//        if (result.v2 != 5) {
//            return result.v2;
//        }

        int relative_direction = findRelativeBot();
        if (relative_direction == 8) {
            return 0;
        }

        Bot other = getBotByDirection(relative_direction);
        if (other == null) {
            return 0;  // just an assertion
        }

        Bot new_bot = new Bot();

//        for (int i = 0; i < MIND_SIZE; i++) {
//            if (rand() > 0.5) {
//                new_bot.mind[i] = this.mind[i];
//            } else {
//                new_bot.mind[i] = other.mind[i];
//            }
//        }
        System.arraycopy(this.mind, 0, new_bot.mind, 0, MIND_SIZE / 2);
        System.arraycopy(other.mind, 0, new_bot.mind, MIND_SIZE / 2, MIND_SIZE / 2);

        for (int i = 0; i < MEMORY_SIZE; i++) {
            if (rand() > 0.5) {
                new_bot.memory[i] = this.memory[i];
            } else {
                new_bot.memory[i] = other.memory[i];
            }
        }

        new_bot.adr = 0;
        new_bot.x = xFromVektorR(n);
        new_bot.y = yFromVektorR(n);

        new_bot.health = (this.health + other.health) / 2;
        this.health = (int) (this.health * 0.5);
        other.health = (int) (other.health * 0.5);

        new_bot.mineral = (this.mineral + other.mineral) / 2;
        this.mineral = (int) (this.mineral * 0.5);
        other.mineral = (int) (other.mineral * 0.5);

        new_bot.alive = LV_ALIVE;

        // incorrect color addition rules, but just as intended
        new_bot.c_red = (this.c_red + other.c_red) / 2;
        new_bot.c_blue = (this.c_blue + other.c_blue) / 2;
        new_bot.c_green = (this.c_green + other.c_green) / 2;

        int ma = (int) (rand() * MIND_SIZE);  // 0..63
        int mc = (int) (rand() * MIND_SIZE);  // 0..63
        new_bot.mind[ma] = (byte) mc;
        new_bot.c_family = getNewColor(c_family);    // цвет семьи вычисляем новый

        new_bot.direction = (int) (rand() * 8);  // направление, куда повернут новорожденный, генерируется случайно

        new_bot.prev = prev;                     // вставляем нового бота между ботом-предком и предыдущим ботом
        prev.next = new_bot;                     // в цепочке ссылок, которая объединяет всех ботов
        new_bot.next = this;
        prev = new_bot;

        World.simulation.matrix[new_bot.x][new_bot.y] = new_bot;    // отмечаем нового бота в массиве matrix
        return 1;
    }


    //....................................................................
    // рождение нового бота делением
    private int botDouble() {
        health -= 150;          // бот затрачивает 150 единиц энергии на создание копии
        if (health <= 0) return 0;        // если у него было меньше 150, то пора помирать

        int n = findEmptyDirection();   // проверим, окружен ли бот
        if (n == 8) {
//            health = 0;  // если бот окружен, то он в муках погибает
            return 0;
        }

        Bot newbot = new Bot();

        int xt = xFromVektorR(n);       // координаты X и Y
        int yt = yFromVektorR(n);

        System.arraycopy(mind, 0, newbot.mind, 0, MIND_SIZE);

        // System.arraycopy(memory, 0, newbot.memory, 0, MEMORY_SIZE / 2);
        // System.arraycopy(mind, MIND_SIZE - MEMORY_SIZE / 2, newbot.memory, MEMORY_SIZE / 2, MEMORY_SIZE / 2);
        // System.arraycopy(mind, MIND_SIZE - MEMORY_SIZE, newbot.memory, 0, MEMORY_SIZE);
        System.arraycopy(memory, 0, newbot.memory, 0, MEMORY_SIZE);

        newbot.adr = 0;                 // указатель текущей команды в новорожденном устанавливается в 0
        newbot.x = xt;
        newbot.y = yt;

        newbot.health = health / 2;     // забирается половина здоровья у предка
        health = health / 2;
        newbot.mineral = mineral / 2;   // забирается половина минералов у предка
        mineral = mineral / 2;

        newbot.alive = LV_ALIVE;        // отмечаем, что бот живой

        newbot.c_red = c_red;           // цвет такой же, как у предка
        newbot.c_green = c_green;       // цвет такой же, как у предка
        newbot.c_blue = c_blue;         // цвет такой же, как у предка
        newbot.c_family = c_family;     // цвет семьи такой же, как у предка

        if (rand() < 0.5) {     // в одном случае из четырех случайным образом меняем один случайный байт в геноме
            for (int i = 0; i < 2; i++) {
                int ma = (int) (rand() * MIND_SIZE);  // 0..63
                int mc = (int) (rand() * MIND_SIZE);  // 0..63
                newbot.mind[ma] = (byte) mc;
            }
            newbot.c_family = getNewColor(c_family);    // цвет семьи вычисляем новый
        }

        newbot.direction = (int) (rand() * 8);  // направление, куда повернут новорожденный, генерируется случайно

        newbot.prev = prev;                     // вставляем нового бота между ботом-предком и предыдущим ботом
        prev.next = newbot;                     // в цепочке ссылок, которая объединяет всех ботов
        newbot.next = this;
        prev = newbot;

        World.simulation.matrix[xt][yt] = newbot;    // отмечаем нового бота в массиве matrix
        return 1;
    }

    private int getNewColor(int parentColor) {
        int r, g, b;
        r = getRed(parentColor);
        g = getGreen(parentColor);
        b = getBlue(parentColor);

        double delta = (((double) 200000 / (World.simulation.generation + 1000)) + 20);

        return getIntColor(vc(r + (int) (rand() * delta - delta / 2)), vc(g + (int) (rand() * delta - delta / 2)), vc(b + (int) (rand() * delta - delta / 2)));
    }


    private int getIntColor(int r, int g, int b) {
        int a = 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int getRed(int color) {
        return (color >> 16) & 0xFF;
    }

    private int getGreen(int color) {
        return (color >> 8) & 0xFF;
    }

    private int getBlue(int color) {
        return (color) & 0xFF;
    }

    private int vc(int c) {
        return c & 0x000000FF;
    }

    // ======       рождение новой клетки многоклеточного    ==========================================
//    private void botMulti(Bot bot) {
//        Bot pbot = bot.mprev;    // ссылки на предыдущего и следущего в многоклеточной цепочке
//        Bot nbot = bot.mnext;
//        // если обе ссылки больше 0, то бот уже внутри цепочки
//        if ((pbot != null) && (nbot != null)) return; // поэтому выходим без создания нового бота
//
//        bot.health = bot.health - 150; // бот затрачивает 150 единиц энергии на создание копии
//        if (bot.health <= 0) return; // если у него было меньше 150, то пора помирать
//        int n = findEmptyDirection(bot); // проверим, окружен ли бот
//
//        if (n == 8) {  // если бот окружен, то он в муках погибает
//            bot.health = 0;
//            return;
//        }
//        Bot newbot = new Bot();
//
//        int xt = xFromVektorR(bot, n);   // координаты X и Y
//        int yt = yFromVektorR(bot, n);
//
//        System.arraycopy(bot.mind, 0, newbot.mind, 0, MIND_SIZE);    // копируем геном в нового бота
//
//        if (rand() < 0.25) {     // в одном случае из четырех случайным образом меняем один случайный байт в геноме
//            int ma = (int) (rand() * MIND_SIZE);  // 0..63
//            int mc = (int) (rand() * MIND_SIZE);  // 0..63
//            newbot.mind[ma] = (byte) mc;
//        }
//
//        newbot.adr = 0;                         // указатель текущей команды в новорожденном устанавливается в 0
//        newbot.x = xt;
//        newbot.y = yt;
//
//        newbot.health = bot.health / 2;   // забирается половина здоровья у предка
//        bot.health = bot.health / 2;
//        newbot.mineral = bot.mineral / 2; // забирается половина минералов у предка
//        bot.mineral = bot.mineral / 2;
//
//        newbot.alive = LV_ALIVE;             // отмечаем, что бот живой
//
//        newbot.c_red = bot.c_red;   // цвет такой же, как у предка
//        newbot.c_green = bot.c_green;   // цвет такой же, как у предка
//        newbot.c_blue = bot.c_blue;   // цвет такой же, как у предка
//
//        newbot.direction = (int) (rand() * 8);   // направление, куда повернут новорожденный, генерируется случайно
//
//        World.simulation.matrix[xt][yt] = newbot;    // отмечаем нового бота в массиве matrix
//
//        if (nbot == null) {                      // если у бота-предка ссылка на следующего бота в многоклеточной цепочке пуста
//            bot.mnext = newbot; // то вставляем туда новорожденного бота
//            newbot.mprev = bot;    // у новорожденного ссылка на предыдущего указывает на бота-предка
//            newbot.mnext = null;       // ссылка на следующего пуста, новорожденный бот является крайним в цепочке
//        } else {                              // если у бота-предка ссылка на предыдущего бота в многоклеточной цепочке пуста
//            bot.mprev = newbot; // то вставляем туда новорожденного бота
//            newbot.mnext = bot;    // у новорожденного ссылка на следующего указывает на бота-предка
//            newbot.mprev = null;       // ссылка на предыдущего пуста, новорожденный бот является крайним в цепочке
//        }
//    }


    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    //========   копится ли энергия            =====
    //========   in - номер бота                =====
    //========   out- 1 - да, 2 - нет           =====
    private int isHealthGrow() {
        int t;
        if (mineral < 100) {
            t = 0;
        } else {
            if (mineral < 400) {
                t = 1;
            } else {
                t = 2;
            }
        }

        int hlt = 0;
        if ((World.simulation.map[x][y] > World.simulation.seaLevel) && (World.simulation.map[x][y] <= World.simulation.seaLevel + 60)) {
            hlt = (World.simulation.seaLevel + 60 - World.simulation.map[x][y]) / 5 + t; // формула вычисления энергии
        }
        if (hlt >= 3) {
            return 1;
        } else {
            return 2;
        }
    }


    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    //========   копятся ли минералы            =====
    //========   in - номер бота                =====
    //========   out- 1 - да, 2 - нет           =====
    private int isMineralGrow() {
        if ((World.simulation.map[x][y] > World.simulation.seaLevel - 30) && (World.simulation.map[x][y] <= World.simulation.seaLevel)) return 1;
        return 2;
    }

    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    //========   родственники ли боты?              =====
    //========   in - номер 1 бота , номер 2 бота   =====
    //========   out- 0 - нет, 1 - да               =====
//    private int isRelative(Bot bot1) {
//        if (bot1.alive != LV_ALIVE) return 0;
//        int dif = 0;    // счетчик несовпадений в геноме
//        for (int i = 0; i < MIND_SIZE; i++) {
//            if (mind[i] != bot1.mind[i]) {
//                dif = dif + 1;
//                if (dif > 1) {
//                    return 0;
//                } // если несовпадений в генеме больше 1
//            }                               // то боты не родственики
//        }
//        return 1;
//    }
    private boolean isRelative(Bot bot1) {
        int rDelta, gDelta, bDelta;
        rDelta = getRed(c_family) - getRed(bot1.c_family);
        gDelta = getGreen(c_family) - getGreen(bot1.c_family);
        bDelta = getBlue(c_family) - getBlue(bot1.c_family);

        int radius2 = rDelta*rDelta + gDelta*gDelta + bDelta*bDelta;

        return  (radius2 < 900);   // магическое число - квадрат расстояния между ботами в пространстве цвета
    }






    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    //=== делаем бота более зеленым на экране         ======
    //=== in - номер бота, на сколько озеленить       ======
    private void goGreen(int num) {  // добавляем зелени
        c_green = c_green + num;
        if (c_green > 255) c_green = 255;
        num = num / 2;
        // убавляем красноту
        c_red = c_red - num;
        if (c_red < 0) c_red = 0;
        // убавляем синеву
        c_blue = c_blue - num;
        if (c_blue < 0) c_blue = 0;

//        c_green = c_green + num;
//        if (c_green + num > 255) {
//            c_green = 255;
//        }
//        int nm = num / 2;
//        // убавляем красноту
//        c_red = c_red - nm;
//        if (c_red < 0) {
//            c_blue = c_blue + c_red;
//        }
//        // убавляем синеву
//        c_blue = c_blue - nm;
//        if (c_blue < 0) c_red = c_red + c_blue;
//        if (c_red < 0) c_red = 0;
//        if (c_blue < 0) c_blue = 0;
    }

    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    //=== делаем бота более синим на экране         ======
    //=== in - номер бота, на сколько осинить       ======
    private void goBlue(int num) {
        // добавляем синевы
        c_blue = c_blue + num;
        if (c_blue > 255) c_blue = 255;
        num = num / 2;
        // убавляем зелень
        c_green = c_green - num;
        if (c_green < 0) c_green = 0;
        // убавляем красноту
        c_red = c_red - num;
        if (c_red < 0) c_red = 0;
    }

    //жжжжжжжжжжжжжжжжжжжхжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжжж
    //=== делаем бота более красным на экране         ======
    //=== in - номер бота, на сколько окраснить       ======
    private void goRed(int num) {  // добавляем красноты
        c_red = c_red + num;
        if (c_red > 255) c_red = 255;
        num = num / 2;
        // убавляем зелень
        c_green = c_green - num;
        if (c_green < 0) c_green = 0;
        // убавляем синеву
        c_blue = c_blue - num;
        if (c_blue < 0) c_blue = 0;
    }


    private double rand() {
        randIdx++;
        if (randIdx >= randMemory.length) {
            randIdx = 0;
        }
        return randMemory[randIdx];
    }


}