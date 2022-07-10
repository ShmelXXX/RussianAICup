import model.*;


public class MyStrategy {

    //    final boolean DEBUGON = true;
//    final boolean DEBUGCONSOLE = false;
    final double K_VISUAL_DEF = 0.4;
    final int MAXMAPSIZE = 80;
    boolean errorflag = false;
//    int [][] emptyArrPlayerEntity = new int [MAXMAPSIZE][MAXMAPSIZE];  // массив определяющий принадлежность юнитов

//    int maxScoutsCount = 3; // кол-во развдчиков

    //    final long MAXUNITCOUNT = 300;
    int myCoordXBase = 0;
    int myCoordYBase = 0;


    long workTime;
    long maxticktime = 0;
    int tick_with_max_time = -1;
    //    int LastBuilderId = 0;
    int tiksWithoutBuild = 0;
    final double K_ATTACK_BASES = 0.7; // 20%
    int sideOfScout = 0;
    Vec2Int lastNearestEnemyPoint = new Vec2Int(79, 79);
    int lastNearestEnemyDist = 1000;
    int curTaskNumb = 0; // текущая задача
    Vec2Int curTaskPos = new Vec2Int(79, 0);
    long realServerTime;


    // поытка вставить объект Если нельзя, то false;
    public boolean checkRangeArr(int x, int y) {
        return (x >= 0) && (x <= MAXMAPSIZE - 1) && (y >= 0) && (y <= MAXMAPSIZE - 1);
    }

    public boolean TryToInsert(EntityType[][] emptyArr, int x, int y, int size) {
//        boolean flag = true;
//        System.out.printf("Size=%2d X=%2d Y=%2d \n", size, x, y );
        if ((x < 0) || (x >= MAXMAPSIZE) || (y < 0) || (y >= MAXMAPSIZE)) {
            return false;
        }

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
//                System.out.println("X="+x+i + "Y="+y+j+"|");
                int promX = x - i;
                int promY = y + j;
                if ((promX < 0) || (promX >= MAXMAPSIZE) || (promY < 0) || (promY >= MAXMAPSIZE)) {
                    return false;
                }
                if (emptyArr[promX][promY] != null) {
                    return false;
                }
            }
        }
        return true;
    }
    /*
        public int FindID(PlayerView playerView, DebugInterface debugInterface) {
            return 0;
        }
    */
    static class Units {
        Vec2Int pos;
        Vec2Int ceil_pos;
        // Рабочие 0-nothing 1-resources 2-build 3-repair
        // Солддаты 0-nothing 1-attack BASE 2-attack UNIT 3-разведка
        int task;
        int Id;
//        Vec2Int ceil;
    }

    class Bases {
        Vec2Int pos;
        int task;  // 0-nothing 1-resources 2-build 3-repair
        int Id;
        int health;
        boolean isActive;
        EntityType type;

//        Vec2Int ceil;
    }

    public int distance(Vec2Int v1, Vec2Int v2) {
/*
        int dx = Math.abs(v1.getX() - v2.getX()) ;
        int dy = Math.abs(v1.getY() - v2.getY()) ;

        double res = 1.0 * dx * dx + 1.0 * dy * dy;
        res = Math.sqrt(res);
*/
//        return (int) Math.round(res);
        return Math.abs(v1.getX() - v2.getX()) + Math.abs(v1.getY() - v2.getY());

    }

    // поиск пустого поля вокруг базы
    public Vec2Int EmptyPlace(EntityType[][] emptyArr, Vec2Int v1, int size) {
        //       Vec2Int prom = new Vec2Int(-1,-1);
//        System.out.printf("EmptyPlace Координаты базы:%2d %2d \n", v1.getX(), v1.getY());
//        System.out.println("Сектор_1");
        for (int i = v1.getX() + size - 1; i > v1.getX(); i--) {
//            System.out.printf("Try Pos %2d:%2d res=%b\n", i, v1.getY()-1+size, TryToInsert(emptyArr, i, v1.getY()-1+size, 1 ));
            if (TryToInsert(emptyArr, i, v1.getY() + size, 1)) {
//                System.out.println("Сектор1");
                return new Vec2Int(i, v1.getY() + size);
            }
        }
//        System.out.println("Сектор_2");
        for (int i = v1.getY() + size - 1; i > v1.getY(); i--) {
            if (TryToInsert(emptyArr, v1.getX() + size, i, 1)) {
//                System.out.println("Сектор2");
                return new Vec2Int(v1.getX() + size, i);
            }
        }
//        System.out.println("Сектор_3");
        for (int i = v1.getX(); i < v1.getX() + size; i++) {
            if (TryToInsert(emptyArr, i, v1.getY() - 1, 1)) {
//                System.out.println("Сектор3");
                return new Vec2Int(i, v1.getY() - 1);
            }
        }
//        System.out.println("Сектор_4");
        for (int i = v1.getY(); i < v1.getY() + size; i++) {
            if (TryToInsert(emptyArr, v1.getX() - 1, i, 1)) {
//                System.out.println("Сектор4");
                return new Vec2Int(v1.getX() - 1, i);
            }
        }

        return new Vec2Int(-1, -1);
    }

    // проверка можно ли поставить базу
    public boolean NearBaseCheck(Vec2Int v1, int baseId, int[][] emptyArrId) {
        int x = v1.getX();
        int y = v1.getY();
//        System.out.printf("\n NearBase. UnitPos=%2d:%2d:Id=%2d \n", x, y, baseId );

        if (x > 0) {
            if (emptyArrId[x - 1][y] == baseId) {
//                System.out.println("check1");
                return true;
            }
        }
//        System.out.println("aftercheck1");
        if (x < MAXMAPSIZE - 1) {
            if (emptyArrId[x + 1][y] == baseId) {
//                System.out.println("check2");
                return true;
            }
        }
//        System.out.println("aftercheck2");
        if (y > 0) {
            if (emptyArrId[x][y - 1] == baseId) {
                //              System.out.println("check3");
                return true;
            }
        }
//        System.out.println("aftercheck3");
        if (y < MAXMAPSIZE - 1) {
            //                System.out.println("check4");
            return emptyArrId[x][y + 1] == baseId;
        }
//        System.out.println("aftercheck4");

        return false;
    }
    public boolean CheckUnderAttackInPoint(int x, int y, int dist, EntityType[][] emptyArr, int[][] arrPlayer, int id) {
        if (checkRangeArr(x, y)) {
            if ((arrPlayer[x][y] != 0) && (arrPlayer[x][y] != id)) { // если чужой юнит
//System.out.printf("Враг замечен:%2d:%2d \n",x,y );
                if ((emptyArr[x][y] == EntityType.RANGED_UNIT) && (dist < 7)) {
                    return true;
                }
                if ((emptyArr[x][y] == EntityType.MELEE_UNIT) && (dist < 4)) {
                    return true;
                }
//                    System.out.printf("Игнорим dist=%2d \n",dist);
            }
        }

        return false;
    }


    public Vec2Int CheckUnderAttack2(Vec2Int v1, EntityType[][] emptyArr, int[][] emptyArrPlayer, int myId) {
        //Если атакуют, то бежать на базу.

//        System.out.printf("Unit:%2d:%1d\n", v1.getX(), v1.getY());
        Vec2Int nearEnemyPos = new Vec2Int(-1, -1);
        for (int i = 1; i < 9; i++) {
            int promX = v1.getX() + i;
            int promY = v1.getY();
            int dist;
            if (CheckUnderAttackInPoint(promX, promY, i, emptyArr, emptyArrPlayer, myId)) {
                nearEnemyPos = new Vec2Int(promX, promY);
                break;
            }
            promX = v1.getX() - i;
            promY = v1.getY();

            if (CheckUnderAttackInPoint(promX, promY, i, emptyArr, emptyArrPlayer, myId)) {
                nearEnemyPos = new Vec2Int(promX, promY);
                break;
            }
            promX = v1.getX();
            promY = v1.getY() + i;
            if (CheckUnderAttackInPoint(promX, promY, i, emptyArr, emptyArrPlayer, myId)) {
                nearEnemyPos = new Vec2Int(promX, promY);
                break;
            }
            promX = v1.getX();
            promY = v1.getY() - i;
            if (CheckUnderAttackInPoint(promX, promY, i, emptyArr, emptyArrPlayer, myId)) {
                nearEnemyPos = new Vec2Int(promX, promY);
                break;
            }

            for (int j = 1; j < i; j++) {
                promX = v1.getX() - j;
                promY = v1.getY() + i - j;
                dist = distance(v1, new Vec2Int(promX, promY));
                if (CheckUnderAttackInPoint(promX, promY, dist, emptyArr, emptyArrPlayer, myId)) {
                    nearEnemyPos = new Vec2Int(promX, promY);
                    break;
                }
                promX = v1.getX() + j;
                promY = v1.getY() + i - j;
                dist = distance(v1, new Vec2Int(promX, promY));
                if (CheckUnderAttackInPoint(promX, promY, dist, emptyArr, emptyArrPlayer, myId)) {
                    nearEnemyPos = new Vec2Int(promX, promY);
                    break;
                }
                promX = v1.getX() + i - j;
                promY = v1.getY() - j;
                dist = distance(v1, new Vec2Int(promX, promY));
                if (CheckUnderAttackInPoint(promX, promY, dist, emptyArr, emptyArrPlayer, myId)) {
                    nearEnemyPos = new Vec2Int(promX, promY);
                    break;
                }
                promX = v1.getX() + i - j;
                promY = v1.getY() + j;
                dist = distance(v1, new Vec2Int(promX, promY));
                if (CheckUnderAttackInPoint(promX, promY, dist, emptyArr, emptyArrPlayer, myId)) {
                    nearEnemyPos = new Vec2Int(promX, promY);
                    break;
                }
            }
            if (nearEnemyPos.getX() != -1) {
                break;
            }

        } // коней цикла поиска врагов

        if (nearEnemyPos.getX() == -1) {
            return nearEnemyPos; // опасности нет
        } else {
            EntityType entity = emptyArr[nearEnemyPos.getX()][nearEnemyPos.getY()];
            int dist = distance(v1, nearEnemyPos);
            if (entity == EntityType.MELEE_UNIT) {
                if (dist == 4) {
                    return v1; // если на границе замри
                }
            }
            if (entity == EntityType.RANGED_UNIT) {
                if (dist == 8) {
                    return v1; // если на границе замри
                }
            }
            int dx;
            int dy;
            dx = Integer.compare(v1.getX(), nearEnemyPos.getX());

            dy = Integer.compare(v1.getY(), nearEnemyPos.getY());
            if ((dx != 0) && (dy != 0)) {
                if (v1.getX() > v1.getY()) {
                    dy = 0;
                } else {
                    dx = 0;
                }
            }
            int promX = v1.getX() + dx;
            int promY = v1.getX() + dx;
            if (checkRangeArr(promX, promY)) {
                return new Vec2Int(promX, promY);
            } else {
                return new Vec2Int(-1, -1); // если не уйти, то не дергаться
            }
        }
    }

//**********************************************************************************************************


    public Action getAction(PlayerView playerView, DebugInterface debugInterface) {
        String log_text = "";
        long startTime = System.currentTimeMillis();
        if (playerView.getCurrentTick() == 0) {
            System.out.println("RussianAiCUP2020  v.34.Round2 (18.12.20)");
            System.out.printf("MapSize=%2d | Туман войны=%b\n", playerView.getMapSize(), playerView.isFogOfWar());
            realServerTime = System.currentTimeMillis();
        }

        Action result = new Action(new java.util.HashMap<>());
        int myId = playerView.getMyId();

//        int populUnits = 0;
//        int counter = 0;
        boolean needRepair = false;
//        boolean needBuildHouse = false;
//        int repairID = 0;
        int housesCount = 0;
        int builderBasesCount = 0;
        int rangedBasesCount = 0;
        int meleeBasesCount = 0;

        int curMeleeUnits = 0;
        int curRangedUnits = 0;
        int curBuilderUnits = 0;
        int maxPopulat = 0;
        int curPopulat;
        double k_builder = 0.0;
        double k_melee = 0.0;
        double k_ranged = 0.0;

        int max_builder;
        int max_melee;
        int max_ranged;

//        Vec2Int repair_coord = new Vec2Int(0, 0);
        Vec2Int scout_ceil = new Vec2Int(MAXMAPSIZE - 1, 0);

        int builderRepairId = 0;
        Vec2Int nearestEnemyUnit = new Vec2Int((int) Math.floor(K_VISUAL_DEF * MAXMAPSIZE), (int) Math.floor(K_VISUAL_DEF * MAXMAPSIZE));
        Vec2Int nearestEnemyBase = new Vec2Int(79, 79);
        int nearestEnemyUnitDist = 1000;
        int nearestEnemyBaseDist = 1000;
        int tryBuildCount = 0; // кол-во попыток построить за ход
        int iToAttackMelee;
        int iToAttackRanged;

        EntityType needBuild = EntityType.RESOURCE; // Если ничего не строить, то ресурс

        Units[] buildersArr = new Units[300];
        Units[] meleeArr = new Units[300];
        Units[] rangeArr = new Units[300];
        Bases MyMeleeBase = new Bases();
        MyMeleeBase.Id = -1;
        MyMeleeBase.isActive = false;
        Bases MyRangedBase = new Bases();
        MyRangedBase.Id = -1;
        MyRangedBase.isActive = false;
        Bases MyBuilderBase = new Bases();
        MyBuilderBase.Id = -1;
        MyBuilderBase.isActive = false;
        Bases repairBase = new Bases();

        EntityType[][] emptyArr = new EntityType[MAXMAPSIZE][MAXMAPSIZE];
        int[][] emptyArrIdEntity = new int[MAXMAPSIZE][MAXMAPSIZE]; // массив определяющий Id сущностей
        int[][] emptyArrPlayerEntity = new int[MAXMAPSIZE][MAXMAPSIZE];  // массив определяющий принадлежность юнитов
        Vec2Int buildBasecoord = new Vec2Int(0, 0);
        int houseInactive = 0;

/*
        for (int i=0; i<playerView.getMapSize()-1; i++)
        {
            for (int j=0; j<playerView.getMapSize()-1; j++){
                emptyArr[i][j] = true;
        }
*/
        /*
        System.out.println();
        for (int i=0; i<playerView.getMapSize()-1; i++)
        {
            for (int j=0; j<playerView.getMapSize()-1; j++){
                if ( emptyArr[i][j] ){
                    System.out.print("1");
                } else {
                    System.out.print("0");
                }
            }
            System.out.println();
            }
*/


//******************************************************************************************************
// Подготовительный цикл
//        System.out.println("Подг.цикл");
        for (Entity entity : playerView.getEntities()) {
            // Если ресурс ничей
            if (entity.getPlayerId() == null) {
                emptyArr[entity.getPosition().getX()][entity.getPosition().getY()] = entity.getEntityType();
                emptyArrIdEntity[entity.getPosition().getX()][entity.getPosition().getY()] = entity.getId();
                continue;
            }


            EntityProperties properties = playerView.getEntityProperties().get(entity.getEntityType());
            int promX = entity.getPosition().getX();
            int promY = entity.getPosition().getY();
            EntityType promType = entity.getEntityType();
            emptyArr[promX][promY] = promType;  // Заполнение пустого массива
            emptyArrPlayerEntity[promX][promY] = entity.getPlayerId();

            // Заполнение массива постройками
            if (properties.getSize() > 1) {
                for (int i = 0; i < properties.getSize(); i++) {
                    for (int j = 0; j < properties.getSize(); j++) {
                        emptyArr[entity.getPosition().getX() + i][entity.getPosition().getY() + j] = entity.getEntityType();
                        emptyArrIdEntity[entity.getPosition().getX() + i][entity.getPosition().getY() + j] = entity.getId();
                        emptyArrPlayerEntity[entity.getPosition().getX() + i][entity.getPosition().getY() + j] = entity.getPlayerId();

                    }
                }
            }
            // Если ресурсы противнике
            if (entity.getPlayerId() != myId) {
                // поиск ближвйшего противника
//                System.out.println("Нашли врага");
                int dist = distance(new Vec2Int(myCoordXBase, myCoordYBase), entity.getPosition());
                if (properties.isCanMove()) {
                    if (dist < nearestEnemyUnitDist) {
                        nearestEnemyUnitDist = dist;
                        nearestEnemyUnit = entity.getPosition();
                    }
                } else {
                    if (dist < nearestEnemyBaseDist) {
                        nearestEnemyBaseDist = dist;
                        nearestEnemyBase = entity.getPosition();
                    }
                }
                if (dist < lastNearestEnemyDist) {
                    lastNearestEnemyDist = dist;
                    lastNearestEnemyPoint = entity.getPosition();
                }
                continue;
            }

            if (entity.getEntityType() == EntityType.HOUSE) {
                housesCount++;
                if (!entity.isActive()) {
                    houseInactive++;
                }

            }
            if (entity.getEntityType() == EntityType.BUILDER_BASE) {
                builderBasesCount++;
                MyBuilderBase.Id = entity.getId();
                MyBuilderBase.isActive = entity.isActive();
                MyBuilderBase.pos = entity.getPosition();
            }
            if (entity.getEntityType() == EntityType.MELEE_BASE) {
                meleeBasesCount++;
                MyMeleeBase.Id = entity.getId();
                MyMeleeBase.isActive = entity.isActive();
                MyMeleeBase.pos = entity.getPosition();
                MyMeleeBase.health = entity.getHealth();

            }
            if (entity.getEntityType() == EntityType.RANGED_BASE) {
                rangedBasesCount++;
                MyRangedBase.Id = entity.getId();
                MyRangedBase.isActive = entity.isActive();
                MyRangedBase.pos = entity.getPosition();
                MyRangedBase.health = entity.getHealth();
            }

            //поиск объекта для ремонта
            if ((!properties.isCanMove()) && (entity.getHealth() < properties.getMaxHealth())) {
                if (!needRepair) {
                    needRepair = true;
                    repairBase.Id = entity.getId();
                    int baseSize = playerView.getEntityProperties().get(entity.getEntityType()).getSize();
//                    System.out.println("baseSize="+baseSize);
                    repairBase.pos = new Vec2Int(entity.getPosition().getX() + (int) Math.floor(0.5 * baseSize), entity.getPosition().getY() + (int) Math.floor(0.5 * baseSize));
                    repairBase.type = entity.getEntityType();
//                    repairID = entity.getId();
//                    repair_coord = new Vec2Int(entity.getPosition().getX()+1, entity.getPosition().getY()+1);
                } else {
                    // Если не дом, то высший приоритет
                    if (entity.getEntityType() != EntityType.HOUSE) {
//                        repairID = entity.getId();
//                        repair_coord = new Vec2Int(entity.getPosition().getX()+1, entity.getPosition().getY()+1);
                        repairBase.Id = entity.getId();
                        int baseSize = playerView.getEntityProperties().get(entity.getEntityType()).getSize();
//                        System.out.println("baseSize="+baseSize);
                        repairBase.pos = new Vec2Int(entity.getPosition().getX() + (int) Math.floor(0.5 * baseSize), entity.getPosition().getY() + (int) Math.floor(0.5 * baseSize));
                        repairBase.type = entity.getEntityType();
                    }
                }
            }


            if (properties.isCanMove()) {
                if ((curTaskPos.getX() == entity.getPosition().getX()) &&
                        curTaskPos.getY() == entity.getPosition().getY()) {
                    System.out.println("Task=" + curTaskNumb + " Complete");
                    curTaskNumb++;
                }
            }

            if (entity.getEntityType() == EntityType.MELEE_UNIT) {
                meleeArr[curMeleeUnits] = new Units();
                meleeArr[curMeleeUnits].pos = entity.getPosition();
                meleeArr[curMeleeUnits].Id = entity.getId();
                meleeArr[curMeleeUnits].ceil_pos = new Vec2Int(0, 0);
                curMeleeUnits++;
            }
            if (entity.getEntityType() == EntityType.RANGED_UNIT) {
                rangeArr[curRangedUnits] = new Units();
                rangeArr[curRangedUnits].pos = entity.getPosition();
                rangeArr[curRangedUnits].Id = entity.getId();
                curRangedUnits++;
            }
            if (entity.getEntityType() == EntityType.BUILDER_UNIT) {
                buildersArr[curBuilderUnits] = new Units();
                buildersArr[curBuilderUnits].pos = entity.getPosition();
                buildersArr[curBuilderUnits].Id = entity.getId();
                curBuilderUnits++;
            }


            // подсчет максимально возможной популяции
            if (entity.isActive()) {
                maxPopulat = maxPopulat + properties.getPopulationProvide();
            }
        }
        // конец подготовительного цикла

        // Ограничение отслеживания вражеских юнитов
        double k = K_VISUAL_DEF;
        if ((playerView.getCurrentTick() > 250) && (nearestEnemyUnitDist != 1000)) {
            k = 1;
        }

        int promX = (int) Math.round(k * MAXMAPSIZE - 1);
        int promY = (int) Math.round(k * MAXMAPSIZE - 1);
//        System.out.printf("near_enemy:%2d:%2d \n", nearestEnemyUnit.getX(), nearestEnemyUnit.getY());
        if ((nearestEnemyUnit.getX() > promX) || (nearestEnemyUnitDist == 1000)) {
            nearestEnemyUnit.setX(promX);
        }
        if ((nearestEnemyUnit.getY() > promY) || (nearestEnemyUnitDist == 1000)) {
            nearestEnemyUnit.setY(promY);
        }

//        System.out.printf("new_point:%2d:%2d \n", promX, promY);

        curPopulat = curMeleeUnits + curBuilderUnits + curRangedUnits;


//        int initCost = playerView.getEntityProperties().get(EntityType.HOUSE).getInitialCost();
//        System.out.println("Проверка условий постройки");
        int deltaUnits = 9;
        int myResource = playerView.getPlayers()[myId - 1].getResource();

        // проверка на постройку дома
        if ((curPopulat + deltaUnits > maxPopulat) && (houseInactive < 3)) {
            if ((MyRangedBase.Id != -1) || (curBuilderUnits < 20)) {
                int initCost = playerView.getEntityProperties().get(EntityType.HOUSE).getInitialCost();
                if (myResource > initCost) {
                    needBuild = EntityType.HOUSE;
                }
            }
        }

        if (MyRangedBase.Id == -1) {
            int initCost = playerView.getEntityProperties().get(EntityType.RANGED_BASE).getInitialCost();
//            System.out.println("Check RangeBase.Cost=" + initCost + "MyRes="+myResource);
            if (myResource > initCost) {
                if ((MyMeleeBase.Id == -1) || (MyMeleeBase.isActive)) {
                    needBuild = EntityType.RANGED_BASE;

//                System.out.println("Try RangeBase");
                }
            }
        }
/*
        if (MyMeleeBase.Id == -1) {
            int initCost = playerView.getEntityProperties().get(EntityType.MELEE_BASE).getInitialCost();
//            System.out.println("Check MeleeBase.Cost=" + initCost+ "MyRes="+myResource);
            if (myResource > initCost){
                if ( (MyRangedBase.Id == -1) || (MyRangedBase.isActive) ) {
                    needBuild = EntityType.MELEE_BASE;
//                System.out.println("Try MeleeBase");
                }
            }
        }
*/
        if (MyBuilderBase.Id == -1) {
            int initCost = playerView.getEntityProperties().get(EntityType.RANGED_BASE).getInitialCost();
//            System.out.println("Check Builde.Cost=" + initCost + "MyRes="+myResource);
            if (myResource > initCost) {
                needBuild = EntityType.BUILDER_BASE;
//                System.out.println("Try BuilderBase");
            }
        }

        if (needBuild != EntityType.RESOURCE) { // поиск лучшегго места
//            System.out.println("Ищем место");
            tiksWithoutBuild++;
//            System.out.println("begin test: tik:"+playerView.getCurrentTick());
            int baseSize = playerView.getEntityProperties().get(needBuild).getSize();
            boolean foundPlaceBuild = false;
            for (int j = 0; j < MAXMAPSIZE - 10; j = j + baseSize + 1) {  // поиск куда можно ставить
                for (int i = 0; i <= j; i = i + baseSize + 1) {
                    int x = i + baseSize - 1;
                    int y = j - i;
//                   System.out.printf("test X=%2d Y=%2d \n", i, i-j + 2-0 );
                    if ((!foundPlaceBuild) && (TryToInsert(emptyArr, x, y, baseSize))) {
//                        System.out.println("Tik="+playerView.getCurrentTick()+"I FOUND:"+x+":"+y);
                        foundPlaceBuild = true;
                        buildBasecoord.setX(x);
                        buildBasecoord.setY(y);
                        break;
//                        System.out.printf("home ceil xx:%2d:%2d  \n", buildBasecoord.getX(), buildBasecoord.getY()) ;
                    }
//                    if (foundPlaceBuild) continue;
                }
                if (foundPlaceBuild) break;
                //               if (foundPlaceBuild) continue;
            }
            //           if (!needBuildHouse) {
//                System.out.println("не нашел места" );
//            }
//        } else {
//            System.out.printf("\n Место не нужно Res=%2d(%2d) !houses=%2d ", playerView.getPlayers()[myId-1].getResource(), initCost, houseInactive);
//        }

            if (needBuild != EntityType.RESOURCE) {
                // поиск ближайшего строителя
                if (tiksWithoutBuild < 40) {
//                if (DEBUGCONSOLE) {
//                    System.out.println("поиск ближ.строителя");
//                }
//                int minDist = MAXMAPSIZE * MAXMAPSIZE;
                    // сортировка по удалению
                    for (int i = 0; i < curBuilderUnits; i++) {
                        for (int j = i + 1; j < curBuilderUnits; j++) {
//                    System.out.println("i=" + i + "j=" + j);
                            if (distance(buildersArr[i].pos, buildBasecoord) > (distance(buildersArr[j].pos, buildBasecoord))) {
                                Units prom = buildersArr[i];
                                buildersArr[i] = buildersArr[j];
                                buildersArr[j] = prom;
                            }
                        }
                    }
                    //               LastBuilderId = buildersArr[0].Id;
                    if (curBuilderUnits > 0) {
                        buildersArr[0].task = 2;
                        buildersArr[0].ceil_pos = buildBasecoord;
                    }
//                System.out.println("поиск ближ.строителя");
                } else {
                    // попытка поставить хоть куда
                    for (int i = 0; i < curBuilderUnits - 1; i++) {  // сортировка по близости к базе
                        int distToCeilI = distance(buildersArr[i].pos, new Vec2Int(0, 0));
                        for (int j = i + 1; j < curBuilderUnits; j++) {
//                    System.out.println("i=" + i + "j=" + j);
                            int distToCeilJ = distance(buildersArr[j].pos, new Vec2Int(0, 0));
                            if (distToCeilI > distToCeilJ) {
                                Units prom = buildersArr[i];
                                buildersArr[i] = buildersArr[j];
                                buildersArr[j] = prom;
                                distToCeilI = distToCeilJ;
                            }
                        }
                    }
                    for (int i = 0; i < curBuilderUnits; i++) {  // попытка поставить поближе
                        if (tryBuildCount < 3) {
                            int x = buildersArr[i].pos.getX();
                            int y = buildersArr[i].pos.getY();
//                   System.out.println("Tik:" +playerView.getCurrentTick()+"Попытка построить2" );
                            baseSize = playerView.getEntityProperties().get(EntityType.HOUSE).getSize();
                            if (TryToInsert(emptyArr, x - baseSize, y, baseSize)) {
//                       System.out.printf("Тик=%3d Попытка построить3 I=%2d X=%2d:Y=%2d| \n", playerView.getCurrentTick(), i,  x, y  );
                                buildersArr[i].task = 2;
                                Vec2Int prom = new Vec2Int(x - (int) Math.floor(0.5 * baseSize), y);
                                buildersArr[i].ceil_pos = prom;
//                       System.out.printf("ceil:%2d:%2d ОК \n", prom.getX(), prom.getY());
                                tryBuildCount++;
                            }

                        }
                    }

                }


            }

//            System.out.println("found builder. Tik=" + playerView.getCurrentTick());
//            System.out.printf("builder xy:%2d:%2d ceil xx:%2d:%2d notbuild:%2d LAST_Id=%2d\n", buildersArr[0].pos.getX(), buildersArr[0].pos.getY(),
//                    buildBasecoord.getX(), buildBasecoord.getY(), tiksWithoutBuild, LastBuilderId );
        }

/*
       // попытка построить хоть что-то
        if (tiksWithoutBuild > 30) {
           for (int i = 0; i < curBuilderUnits; i++) {  // сортировка по близости к базе
               for (int j = i + 1; j < curBuilderUnits; j++) {
//                    System.out.println("i=" + i + "j=" + j);
                   if (distance(buildersArr[i].pos, new Vec2Int(myCoordXBase, myCoordYBase)) > (distance(buildersArr[j].pos, repairBase.pos))) {
                       Units prom = buildersArr[i];
                       buildersArr[i] = buildersArr[j];
                       buildersArr[j] = prom;
                   }
               }
           }
           for (int i = 0; i < curBuilderUnits; i++) {  // попытка поставить поближе
               if (tryBuildCount < 3) {
                   int x = buildersArr[i].pos.getX();
                   int y = buildersArr[i].pos.getY();
//                   System.out.println("Tik:" +playerView.getCurrentTick()+"Попытка построить2" );
                   if (TryToInsert(emptyArr, x - 3, y, 3, playerView.getMapSize())) {
//                       System.out.printf("Тик=%3d Попытка построить3 I=%2d X=%2d:Y=%2d| \n", playerView.getCurrentTick(), i,  x, y  );
                        buildersArr[i].task =2;
                        Vec2Int prom = new Vec2Int(x-1,y);
                        buildersArr[i].ceil_pos = prom;
//                       System.out.printf("ceil:%2d:%2d ОК \n", prom.getX(), prom.getY());
                       tryBuildCount ++;
                   }
               }
           }
       }
*/

        // Выбор строителей для ремонта
        if (needRepair) {
//           int minDist = 1000;
            // сортировка по удалению
            for (int i = 0; i < curBuilderUnits - 1; i++) {
                int promdist1 = distance(buildersArr[i].pos, repairBase.pos);
                for (int j = i + 1; j < curBuilderUnits; j++) {
//                    System.out.println("i=" + i + "j=" + j);
                    int promdist2 = distance(buildersArr[j].pos, repairBase.pos);
                    if (promdist1 > promdist2) {
                        Units prom = buildersArr[i];
                        buildersArr[i] = buildersArr[j];
                        buildersArr[j] = prom;
                        promdist1 = promdist2;
                    }
                }
            }
//                for (int i = 0; i < curBuilderUnits; i++) {
//                    System.out.println("after :" + i + "=" + i + " Id="+ buildersArr[i].Id + " dist= " + distance(buildersArr[i].pos, repair_coord));
//                }
            // Ограничение количества ремонтников
            int builderRepairCount = (int) Math.round(0.25 * curBuilderUnits);
            if (builderRepairCount > 5) {
                if (repairBase.type == EntityType.HOUSE) {
                    builderRepairCount = 5;
                } else {
                    if (builderRepairCount > 8) {
                        builderRepairCount = 8;
                    }
                }
            }
            if ((builderRepairCount == 1) && (curBuilderUnits > 4)) {
                builderRepairCount = 2;
            }

            for (int i = 0; i < builderRepairCount; i++) {
                if (buildersArr[i].task != 2) {
                    buildersArr[i].task = 3;
                } else {
//                     buildersArr[builderRepairCount].task = 3;
                }
            }
        }
        for (int i = 0; i < curBuilderUnits; i++) {
            if (buildersArr[i].task != 3) {
                Vec2Int v = CheckUnderAttack2(buildersArr[i].pos, emptyArr, emptyArrPlayerEntity, myId);
                if (v.getX() != -1) {
                    buildersArr[i].task = 4;
                    buildersArr[i].ceil_pos = v;
//                   System.out.println("Unit under attack:"+ buildersArr[i].pos.getX()+":"+buildersArr[i].pos.getY());
                }


            }
        }


//            System.out.print("test" + buildersArr[0].getCurrentTick() + "X");

        if (playerView.getCurrentTick() < 100000) {
//            if (curBuilderUnits>10) {
            k_builder = 0.66;
            k_melee = 0.0;
            k_ranged = 0.33;
//            } else {
//                k_builder = 0.5;
//                k_melee = 0.0;
//                k_ranged = 0.25;

//            }
        }
        if (playerView.getCurrentTick() < 400) {
            k_builder = 0.66;
            k_melee = 0.0;
            k_ranged = 0.33;
        }
        if (playerView.getCurrentTick() < 50) {
            k_builder = 1.0;
            k_melee = 0.0;
            k_ranged = 0.0;
        }


        max_melee = (int) Math.round(k_melee * maxPopulat);
        max_ranged = (int) Math.round(k_ranged * maxPopulat);
        max_builder = (int) Math.round(k_builder * maxPopulat);

        double d_melee = max_melee;
        double d_ranged = max_ranged;
        double d_builder = max_builder;
        if (curMeleeUnits != 0) {
            d_melee = 1.0 * max_melee / curMeleeUnits;
        }

        if (curRangedUnits != 0) {
            d_ranged = 1.0 * max_ranged / curRangedUnits;
        }
        if (curBuilderUnits != 0) {
            d_builder = 1.0 * max_builder / curBuilderUnits;
        }
        int tryBuild = 0;
        if ((d_builder >= d_melee) && (d_builder >= d_ranged)) {
            tryBuild = 0;
        }
        if ((d_melee >= d_builder) && (d_melee >= d_ranged)) {
            tryBuild = 1;
        }
        if ((d_ranged >= d_builder) && (d_ranged >= d_melee)) {
            tryBuild = 2;
        }

        // Если строителей меньше 3 принудительно строить
        if (curBuilderUnits < 3) {
            tryBuild = 0;
        }


        // Если Туман войны - идти по точкам
        if (playerView.isFogOfWar()) {
            if (nearestEnemyBaseDist == 1000) {
//                System.out.print("Выполнение задания");
                if (curTaskNumb > 2) {
                    curTaskNumb = 0;
                }
                if (curTaskNumb == 0) {
                    nearestEnemyBase = new Vec2Int(0, MAXMAPSIZE - 1 - 5);
                }
                if (curTaskNumb == 1) {
                    nearestEnemyBase = new Vec2Int(MAXMAPSIZE - 1 - 5, MAXMAPSIZE - 1 - 5);
                }
                if (curTaskNumb == 2) {
                    nearestEnemyBase = new Vec2Int(MAXMAPSIZE - 1 - 5, 0);

                }
                curTaskPos = nearestEnemyBase;
                nearestEnemyBaseDist = distance(new Vec2Int(0, 0), nearestEnemyBase);
            }




/*
            if (curMeleeUnits+curRangedUnits  > 10){
                System.out.print("Разведка");
                if (sideOfScout >2) {
                    sideOfScout = 0;
                }
                System.out.println("\n Направление разведки = " + sideOfScout);
                if (sideOfScout == 0) {
                    scout_ceil = new Vec2Int(playerView.getMapSize()-1, 0 );
                }
                if (sideOfScout == 1) {
                    scout_ceil = new Vec2Int(playerView.getMapSize()-1, playerView.getMapSize()-1);
                }
                if (sideOfScout == 2) {
                    scout_ceil = new Vec2Int(0, playerView.getMapSize()-1);
                }
                sideOfScout++;

                int curScoutsCount = 0; // кол-во развдчиков
                int rangedScoutsCount = 0;
                int meleeScoutsCount = 0;

                for (int i= 0; curScoutsCount <= maxScoutsCount; i++ ){
                    if (meleeScoutsCount < curMeleeUnits) {
                        meleeArr[meleeScoutsCount].task = 3;
                        meleeArr[meleeScoutsCount].ceil_pos = scout_ceil ;
                        meleeScoutsCount++;
                        curScoutsCount++;
                    }
                    if (rangedScoutsCount < curRangedUnits) {
                        meleeArr[rangedScoutsCount].task = 3;
                        meleeArr[rangedScoutsCount].ceil_pos = scout_ceil ;
                        rangedScoutsCount++;
                        curScoutsCount++;
                    }

                }

            }
 */
        }
        if (!playerView.isFogOfWar()) {
//                if (nearestEnemyBaseDist == 1000) {
//            System.out.println("Выполнение задания");
            if (curTaskNumb > 2) {
                curTaskNumb = 0;
            }
            if (curTaskNumb == 0) {
                nearestEnemyBase = new Vec2Int(0, MAXMAPSIZE - 1 - 5);
            }
            if (curTaskNumb == 1) {
                nearestEnemyBase = new Vec2Int(MAXMAPSIZE - 1 - 5, MAXMAPSIZE - 1 - 5);
            }
            if (curTaskNumb == 2) {
                nearestEnemyBase = new Vec2Int(MAXMAPSIZE - 1 - 5, 0);

            }
            curTaskPos = nearestEnemyBase;
            nearestEnemyBaseDist = distance(new Vec2Int(0, 0), nearestEnemyBase);
        }

        // Солддаты 0-attack UNIT  1-attack BASE 2-разведка
        double curKAttackBases = K_ATTACK_BASES;
        if ((playerView.getCurrentTick() < 100) || (curMeleeUnits + curRangedUnits < 15)) {
            curKAttackBases = 0;
        }
        iToAttackMelee = (int) Math.round(curMeleeUnits * curKAttackBases);
        for (int i = 0; i < curMeleeUnits; i++) {
            if (i < iToAttackMelee) {
                meleeArr[i].task = 1;
                meleeArr[i].ceil_pos = nearestEnemyBase;
            } else {
                meleeArr[i].task = 0;
                meleeArr[i].ceil_pos = nearestEnemyUnit;
            }
        }

        iToAttackRanged = (int) Math.round(curRangedUnits * curKAttackBases);
        for (int i = 0; i < curRangedUnits; i++) {
            if (i < iToAttackRanged) {
                rangeArr[i].task = 1;
                rangeArr[i].ceil_pos = nearestEnemyBase;
            } else {
                rangeArr[i].task = 0;
                rangeArr[i].ceil_pos = nearestEnemyUnit;
            }
        }

// builder = 0
// melee = 1
// ranged =2
//        System.out.println();
//        System.out.println("BeginTryBuildUnit = " + tryBuild);
        if ((tryBuild == 0) && (builderBasesCount == 0)) {
            tryBuild = 1;
        }
        boolean changeUnit = false;
        if (tryBuild == 1) {
            if (!MyMeleeBase.isActive) {
                changeUnit = true;
                tryBuild = 2;
            }
        }

//        System.out.println(" MyRamgeBase.isActive=" + MyRangedBase.isActive + " MyMeleeBase=" + MyMeleeBase.isActive + " MyBuilderBase=" + MyBuilderBase.isActive);
        if (tryBuild == 2) {
            if ((MyRangedBase.Id == -1) || (!MyRangedBase.isActive)) {
                changeUnit = true;
                tryBuild = 1;
            }
        }

        if (((!MyRangedBase.isActive)) &&
                (!MyMeleeBase.isActive)) {
            tryBuild = 0;
        }

/*
        if (curBuilderUnits >10)  {  // тестовый кусок
            tryBuild =5;
        }  else {
            tryBuild = 0;
        }
*/


        if ((playerView.getCurrentTick() + 1) % 50 == 0) {
            System.out.print("Tik=" + playerView.getCurrentTick());
            System.out.printf("(%3d|%3d(t=%2d|ServerTime=%2d))", workTime, maxticktime, tick_with_max_time, (System.currentTimeMillis()) - realServerTime);
            System.out.printf("|TryBU=%d", tryBuild);
//            System.out.printf(" B=%2d",curBuilderUnits);
//            System.out.printf(" M=%2d(%2d)",curMeleeUnits, iToAttackMelee);
//            System.out.printf(" R=%2d(%2d)",curRangedUnits, iToAttackRanged);
//            System.out.printf(" Scouts:%d", maxScoutsCount);
//            System.out.print(" Popul=" + curPopulat);
//            System.out.print("(" + maxPopulat + ")");
            System.out.print(" needBuild=" + needBuild);
            System.out.print(" needRepair=" + needRepair);
            System.out.print(" NoBuildT=" + tiksWithoutBuild);
            System.out.printf("|CurTaskNumb=%d (pos:%2d:%2d)  ", curTaskNumb, curTaskPos.getX(), curTaskPos.getY());
//            if (MyMeleeBase.Id != -1)  System.out.printf(" |MeleeBase.pos:%2d:%2d:Id=%2d:isActive=%b:Health=%2d",
//                    MyMeleeBase.pos.getX(), MyMeleeBase.pos.getY(), MyMeleeBase.Id, MyMeleeBase.isActive,MyMeleeBase.health);
//            if (MyRangedBase.Id != -1)  System.out.printf(" |RangedBase.pos:%2d:%2d:Id=%2d:isActive=%b:Health=%2d",
//                    MyRangedBase.pos.getX(), MyRangedBase.pos.getY(), MyRangedBase.Id, MyRangedBase.isActive,MyRangedBase.health);
            System.out.printf(" |n_b:%2d n_m:%2d n_r:%2d", max_builder, max_melee, max_ranged);
            System.out.printf(" |d_b:%2.1f d_m:%2.1f d_r:%2.1f ", d_builder, d_melee, d_ranged);
            System.out.printf(" |NearEnemBase:%2d:%2d:dist=%2d|NearEnemUnit:%2d:%2d:dist=%2d|", nearestEnemyBase.getX(), nearestEnemyBase.getY(), nearestEnemyBaseDist,
                    nearestEnemyUnit.getX(), nearestEnemyUnit.getY(), nearestEnemyUnitDist);
//            System.out.printf("|MyBase:%2d:%2d| \n", myCoordXBase, myCoordYBase);
            System.out.println();
        }
/*
//        System.out.println("TikTak:"+playerView.getCurrentTick()+" count="+count+" mapsize=" + playerView.getMapSize());
        for (int i=0; i<playerView.getMapSize(); i++)
        {
            System.out.printf("%2d ", i);
            for (int j=0; j<playerView.getMapSize(); j++){
                System.out.print(emptyArr[i][j]);
            }
            System.out.println();
        }
*/


/*
        System.out.print("Build:");
            for (int i = 0; i < curBuilderUnits; i++) {
                System.out.print(buildersArr[i].task);
            }
            System.out.println();

        System.out.print("Melee:");
        for (int i = 0; i < curMeleeUnits; i++) {
            System.out.print(meleeArr[i].task);
        }
        System.out.println();
        System.out.print("Range:");
        for (int i = 0; i < curRangedUnits; i++) {
            System.out.print(rangeArr[i].task);
        }
        System.out.println();
*/


//        System.out.println("Конец подг.цикла");


//******************************************************************************************************
// Главный цикл
        for (Entity entity : playerView.getEntities()) {
            if (entity.getPlayerId() == null || entity.getPlayerId() != myId) {
                continue;
            }
            if (playerView.getCurrentTick() == 0) {
                if (entity.getEntityType() == EntityType.BUILDER_BASE) {
                    if (entity.getPosition().getX() > (int) (0.5 * MAXMAPSIZE)) {
                        myCoordXBase = MAXMAPSIZE - 1;
                    } else {
                        myCoordXBase = 0;
                    }
                    if (entity.getPosition().getY() > (int) (0.5 * MAXMAPSIZE)) {
                        myCoordYBase = MAXMAPSIZE - 1;
                    } else {
                        myCoordYBase = 0;
                    }
                }
            }

            EntityProperties properties = playerView.getEntityProperties().get(entity.getEntityType());

//                System.out.print(" MaxHealth=" + properties.getMaxHealth());
//                System.out.println(" PosX=" + entity.getPosition().getX() + " PosY=" + entity.getPosition().getY());

            MoveAction moveAction = null;
            BuildAction buildAction = null;
            RepairAction repairAction = null;
            EntityType[] validAutoAttackTargets;

            if (entity.getEntityType() == EntityType.BUILDER_UNIT) {
                validAutoAttackTargets = new EntityType[]{EntityType.RESOURCE};
            } else {
                validAutoAttackTargets = new EntityType[0];
            }

            AttackAction attackAction = new AttackAction(null, new AutoAttack(properties.getSightRange(), validAutoAttackTargets));

            if (properties.isCanMove()) {
                moveAction = new MoveAction(
                        new Vec2Int(MAXMAPSIZE - 1, MAXMAPSIZE - 1),
                        true,
                        true);
/*
                // Если не строитель двигаться к ближайшему противнику
/*
                if (entity.getEntityType() != EntityType.BUILDER_UNIT) {
                    moveAction = new MoveAction(
                            nearestEnemyUnit,
                            true,
                            true);
                }
*/
            } else if (properties.getBuild() != null) {
                EntityType entityType = properties.getBuild().getOptions()[0];
                int currentUnits = 0;
                for (Entity otherEntity : playerView.getEntities()) {
                    if (otherEntity.getPlayerId() != null && otherEntity.getPlayerId() == myId
                            && otherEntity.getEntityType() == entityType) {
                        currentUnits++;
                    }
                }


//                if ((playerView.getCurrentTick() > 200) || (entity.getEntityType() != EntityType.RANGED_BASE)) {
/*
                        if ((currentUnits + 1) * playerView.getEntityProperties().get(entityType).getPopulationUse() <= properties.getPopulationProvide()) {
                            buildAction = new BuildAction(
                                    entityType,
                                    new Vec2Int(
                                            entity.getPosition().getX() + properties.getSize(),
                                            entity.getPosition().getY() + properties.getSize() - 1
                                    )
                            );
                        }

 */
//                }

            }


//            попытка построить базу
//            int myresources = playerView.getPlayers()[myId - 1].getResource();
//            int house_cost = playerView.getEntityProperties().get(EntityType.HOUSE).getInitialCost();
            if ((entity.getEntityType() == EntityType.BUILDER_UNIT)) {

                int builderIndex = 0;

                for (int i = 0; i < curBuilderUnits; i++) { // поиск рабочего в массиве
                    if ((entity.getId() == buildersArr[i].Id)) {
                        builderIndex = i;
                        break;
                    }
                }

// сваливаем строителем
                if ((buildersArr[builderIndex].task == 4)) {
                    moveAction = new MoveAction(buildersArr[builderIndex].ceil_pos, true, false);
//                    System.out.printf("\nПобег Bpos=:%2d:%2d ceil=%2d:%2d \n",buildersArr[builderIndex].pos.getX(),buildersArr[builderIndex].pos.getY(),
//                            buildersArr[builderIndex].ceil_pos.getX(), buildersArr[builderIndex].ceil_pos.getY());
                    repairAction = null;
                    attackAction = null;
                }
            }

            if ((entity.getEntityType() == EntityType.BUILDER_UNIT) && (needBuild != null)) {

                int builderIndex = 0;

                for (int i = 0; i < curBuilderUnits; i++) { // поиск рабочего в массиве
                    if ((entity.getId() == buildersArr[i].Id)) {
                        builderIndex = i;
                        break;
                    }
                }

 /*
                int i=0;
                while (i<curBuilderUnits){
                    if ((builderIndex == 0) && (entity.getId() == buildersArr[i].Id)){
                        builderIndex = i;
                        i=curBuilderUnits;
                    } else {
                        i++;
                    }
                }
*/

                if ((buildersArr[builderIndex].task == 2)) {   // try to Build
//                    int dist = distance(entity.getPosition(), new Vec2Int(buildersArr[builderIndex].ceil_pos.getX()+1, buildersArr[builderIndex].ceil_pos.getY()));
//                    System.out.printf("|Try Build uniId:%2d(%2d:%2d) ceil:%2d:%2d Dist:%2d \n ", entity.getId(), entity.getPosition().getX(), entity.getPosition().getY(),
//                            buildBasecoord.getX(), buildBasecoord.getY(), dist);
                    /*if (DEBUGCONSOLE)*/
                    {
//                        System.out.printf("|Тик=%2d|Try Build uniId:%2d(%2d:%2d) ceil:%2d:%2d Dist:%2d \n", playerView.getCurrentTick(), entity.getId(), entity.getPosition().getX(), entity.getPosition().getY(),
//                                buildersArr[builderIndex].ceil_pos.getX(),buildersArr[builderIndex].ceil_pos.getY(), repair_coord.getY(), dist);
                    }
//                    if ((entity.getPosition().getX()==buildBasecoord.getX()+1) && (entity.getPosition().getY()==buildBasecoord.getY())) {
                    if ((entity.getPosition().getX() == buildersArr[builderIndex].ceil_pos.getX() + 1) &&
                            (entity.getPosition().getY() == buildersArr[builderIndex].ceil_pos.getY())) {
                        moveAction = null;
                        attackAction = null;
                        tiksWithoutBuild = 0;
//                        System.out.printf("BUILD\n");
                        int baseSize = playerView.getEntityProperties().get(needBuild).getSize();
                        buildAction = new BuildAction(
                                needBuild,
                                new Vec2Int(
                                        entity.getPosition().getX() - baseSize,
                                        entity.getPosition().getY()));
//                        if (DEBUGCONSOLE) {
//                            System.out.println("build:" + needBuild);
//                        }

                    } else {
//                        if (DEBUGCONSOLE) {
//                            System.out.printf("GO\n");
//                        }
                        moveAction = new MoveAction(new Vec2Int(buildBasecoord.getX() + 1, buildBasecoord.getY()), true, false);
                        repairAction = null;
                        attackAction = null;
//                        if (DEBUGCONSOLE) {
//                           System.out.println("go to ceil");
//                        }
                    }
//                    if (DEBUGCONSOLE) {
//                        System.out.print("|");
//                   }
                }

            }

            if (entity.getEntityType() == EntityType.MELEE_UNIT) {
//                System.out.println("Обработка мечника");
                int index = 0;
                for (int i = 0; i < curMeleeUnits; i++) { // поиск мечника в массиве
//                    System.out.println("i=" + i);
                    if (entity.getId() == meleeArr[i].Id) {
                        index = i;
                        break;
                    }
                }
//                    System.out.println( "Tik="+ playerView.getCurrentTick() + "MoveMelee Ceil="+ meleeArr[index].ceil_pos.getX() +":"+ meleeArr[index].ceil_pos.getY());

                moveAction = new MoveAction(
                        meleeArr[index].ceil_pos,
                        true,
                        true);
//                System.out.println("  Tik="+ playerView.getCurrentTick() + "end move Melee");

            }

            if (entity.getEntityType() == EntityType.RANGED_UNIT) {

                int index = 0;

                for (int i = 0; i < curRangedUnits; i++) { // поиск мечника в массиве
                    if (entity.getId() == rangeArr[i].Id) {
                        index = i;
                        break;
                    }
                }

//                System.out.println(" Tik="+ playerView.getCurrentTick() + "MoveRanged Ceil="+ rangeArr[index].ceil_pos.getX() +":"+ rangeArr[index].ceil_pos.getY());

                moveAction = new MoveAction(
                        rangeArr[index].ceil_pos,
                        true,
                        true);
//                System.out.println("  Tik="+ playerView.getCurrentTick() + "end move Ranged");

            }
/*
//            попытка построить базу строителей
                 int builder_base_cost = playerView.getEntityProperties().get(EntityType.BUILDER_BASE).getInitialCost();
                log_text = log_text + "House="+ housesCount;
                System.out.println("Builder_base_Cost="+builder_base_cost);
                if ((entity.getEntityType() == EntityType.BUILDER_UNIT) &&
                        (myresources >= builder_base_cost) &&
                        (housesCount>0) ){
                    log_text = log_text + " try_build_BUILDERBASE";
                    EntityType entityType = EntityType.BUILDER_BASE;
                    buildAction = new BuildAction(
                            EntityType.BUILDER_BASE,
                            new Vec2Int(
                                    entity.getPosition().getX() + properties.getSize(),
                                    entity.getPosition().getY() + properties.getSize() - 1
                            )
                    );
                }
*/
//**********************************************************************************************************************
//          попытка починить
            if ((entity.getEntityType() == EntityType.BUILDER_UNIT) &&
                    (needRepair)) {
                int builderIndex = 0;
                for (int i = 0; i < curBuilderUnits; i++) {
                    if (entity.getId() == buildersArr[i].Id) {
                        builderIndex = i;
                        break;
                    }
                }

                if (buildersArr[builderIndex].task == 3) {   // try to repair
                    int dist = distance(entity.getPosition(), repairBase.pos);

                    int baseSize = playerView.getEntityProperties().get(repairBase.type).getSize();
//                    System.out.printf("\n REPAIR Builder:pos%2d:%2d| dist=%2d | Base(pos:%2d:%2d) Size=%2d", entity.getPosition().getX(), entity.getPosition().getY(), dist,
//                            repairBase.pos.getX(), repairBase.pos.getY(), baseSize);
//     public boolean NearBaseCheck (Vec2Int v1, int baseId, int[][] emptyArrId, int maxMapSize ) {
                    boolean flag = NearBaseCheck(entity.getPosition(), repairBase.Id, emptyArrIdEntity);
//                    System.out.print("NearBase=" + flag );
                    if (!flag) {
                        moveAction = new MoveAction(repairBase.pos, true, false);
                        repairAction = null;
                        attackAction = null;
//                        if (DEBUGCONSOLE) {
//                            System.out.print(" repair go to ceil");
//                        }
                    } else {
                        repairAction = new RepairAction(repairBase.Id);
//                        if (DEBUGCONSOLE) {
//                            System.out.print(" repair");
//                        }
                    }
//System.out.println();
//                    if (DEBUGCONSOLE) {
//                        System.out.print("|");
//                    }

                }
            }


            //          попытка построить лучника
            if (entity.getEntityType() == EntityType.RANGED_BASE) {
//                System.out.println("строим лучника");
                if (tryBuild == 2) {
                    int initCost = playerView.getEntityProperties().get(EntityType.RANGED_UNIT).getInitialCost();
                    if ((curPopulat < maxPopulat) && (myResource >= initCost + curRangedUnits)) {

/*
                        buildAction = new BuildAction(
                                EntityType.RANGED_UNIT,
                                new Vec2Int(
                                        entity.getPosition().getX() + properties.getSize(),
                                        entity.getPosition().getY() + properties.getSize() - 1
                                )
                        );
                        if (DEBUGCONSOLE) {
                            System.out.print(" tb_R ");
                        }
                    }

 */

                        Vec2Int buildPosition = EmptyPlace(emptyArr, MyRangedBase.pos, properties.getSize());
//                        System.out.printf("PosToBuild:%2d:%2d \n", buildPosition.getX(), buildPosition.getY());
                        if (buildPosition.getX() >= 0) {
                            buildAction = new BuildAction(
                                    EntityType.RANGED_UNIT,
                                    buildPosition);

//                            if (DEBUGCONSOLE) {
//                                System.out.print(" tb_B");
//                            }
                        }
                    }
                }
            }


            //          попытка построить мечника
            if (entity.getEntityType() == EntityType.MELEE_BASE) {
                if (tryBuild == 1) {
                    if (curPopulat < maxPopulat) {
                        int initCost = playerView.getEntityProperties().get(EntityType.MELEE_UNIT).getInitialCost();
                        if ((curPopulat < maxPopulat) && (myResource >= initCost + curMeleeUnits)) {
                            Vec2Int buildPosition = EmptyPlace(emptyArr, MyMeleeBase.pos, properties.getSize());
//                        System.out.printf("PosToBuild:%2d:%2d \n", buildPosition.getX(), buildPosition.getY());
                            if (buildPosition.getX() >= 0) {
                                buildAction = new BuildAction(
                                        EntityType.MELEE_UNIT,
                                        buildPosition);

//                                if (DEBUGCONSOLE) {
//                                    System.out.print(" tb_B");
//                                }
                            }
/*
                        System.out.println("строим мечника3");
                        buildAction = new BuildAction(
                                EntityType.MELEE_UNIT,
                                new Vec2Int(
                                        entity.getPosition().getX() + properties.getSize(),
                                        entity.getPosition().getY() + properties.getSize() - 1
                                )
                        );
//                        System.out.println("конецстроим мечника3");
                        if (DEBUGCONSOLE) {
                            System.out.print(" tb_M ");
                        }
                    }

 */


                        }
                    }
                }
            }


            //          попытка построить строителя
            if (entity.getEntityType() == EntityType.BUILDER_BASE) {
//         int myResource = playerView.getPlayers()[myId-1].getResource();
                if (tryBuild == 0) {
                    int initCost = playerView.getEntityProperties().get(EntityType.BUILDER_UNIT).getInitialCost();
                    if ((curPopulat < maxPopulat) && (myResource >= initCost + curBuilderUnits)) {

                        Vec2Int buildPosition = EmptyPlace(emptyArr, MyBuilderBase.pos, properties.getSize());
//                        System.out.printf("PosToBuild:%2d:%2d \n", buildPosition.getX(), buildPosition.getY());
                        if (buildPosition.getX() >= 0) {
                            buildAction = new BuildAction(
                                    EntityType.BUILDER_UNIT,
                                    buildPosition);

//                            if (DEBUGCONSOLE) {
//                                System.out.print(" tb_B");
//                            }
                        } else {
                            if (errorflag == false) {
                                System.out.printf(" Tik=%2d|Can't build|Bpos=%2d:%2d", playerView.getCurrentTick(), MyBuilderBase.pos.getX(), MyBuilderBase.pos.getY());
                                errorflag = true;
                            }
                        }

                    }
                }
            }

//            System.out.println("EntityPut");
            result.getEntityActions().put(entity.getId(), new EntityAction(
                    moveAction,
                    buildAction,
                    attackAction,
                    repairAction
            ));

//            System.out.println("EntityPutEnd");

        }

//            log_text = log_text + " needRepair="+needRepair;


//        Vec2Float vec1 = new Vec2Float(0, 0);
//        Vec2Float vec2 = new Vec2Float(0, 0);
//        String text = "Hello" + playerView.getCurrentTick();
//        ColoredVertex colVertex = new ColoredVertex(vec1, vec2, new Color(1, 1, 1, 1));
//        DebugCommand debugCommand = new DebugCommand.Add(new DebugData.PlacedText(colVertex, text, 0, 20));
//        debugCommand = new DebugCommand.Add(new DebugData.PlacedText(colVertex, log_text, 1, 20));
        //           debugInterface.send(debugCommand);

//        debugCommand = new DebugCommand.Add(new DebugData.Log(log_text));
//            debugInterface.send(debugCommand);
//        if (playerView.getCurrentTick() % 50 == 0) {
//            System.out.println();
//        }
        long taktTime = System.currentTimeMillis() - startTime;
        workTime = workTime + taktTime;
        if (taktTime > maxticktime) {
            maxticktime = taktTime;
            tick_with_max_time = playerView.getCurrentTick();
        }

        return result;
    }


    public void debugUpdate(PlayerView playerView, DebugInterface debugInterface) {
        debugInterface.send(new DebugCommand.Clear());
        debugInterface.getState();
    }
}