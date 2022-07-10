import model.*;

// контроль прицела чуть ниже
// не надо прыгать, если есть прямая дорога
// держать дистанцию
// 19 если давно не стреляли - сближаться.

public class MyStrategy {
//  static  boolean test_flag = true;
  static boolean print_console = true;
  static boolean draw_screen = true;
  static  boolean test_flag = false;
//  int targ = 0;
  enum Targs {
    HealthPack,
    NearestEnemy,
    Weapon,
    RunAway,

  }
  Targs targ;



  boolean after_first_tick=false;
  boolean see_enemy = false;
  static ColorFloat cl_blue = new ColorFloat((float) 0, 0, 255, (float) 1.0);
  static ColorFloat cl_red = new ColorFloat((float) 255, 0, 0, (float) 0.4);
  static ColorFloat cl_redred = new ColorFloat((float) 255, 0, 0, (float) 1.0);
  static ColorFloat cl_green = new ColorFloat((float) 0, 255, 0, (float) 1.0);
  static ColorFloat cl_white = new ColorFloat((float) 255, 255, 255, (float) 0.2);
  static ColorFloat cl_whitewhite = new ColorFloat((float) 255, 255, 255, (float) 1);
  Vec2Float v1;
  Vec2Float v2;
  Vec2Float v3;
  static  double min_chance_to_shot = 0.5;
  int jumptime = 0; // сколько микротиков в воздухе


  static double distanceSqr(Vec2Double a, Vec2Double b) {
    return (a.getX() - b.getX()) * (a.getX() - b.getX()) + (a.getY() - b.getY()) * (a.getY() - b.getY());
  }

  static double distance_abs(Vec2Double a, Vec2Double b) {
    return Math.sqrt(distanceSqr(a,b));
  }

  //***********************************
  static double woundProcent = 0.7;

  long time_longest_tick = -1;
  long longest_tick = -1;
  boolean myflag = false;
  Tile [][] arr;
  static int unitscoordsize = 4;
  double [][] unitscoord;
  double [][] bulletscoord;

  String promstr = "";
  boolean longtimebetweenshoots = false;
  int ActiveMineTimer;


  public double ingradus(double radians){
    return radians*180/Math.PI;
  }

// ***********************************

  public UnitAction getAction(Unit unit, Game game, Debug debug) {

    // Первый запуск
    if (!after_first_tick) {  // первый тик
      System.out.println("14/12/19 v4.22.20");
      if (test_flag) {
        System.out.println("ТЕСТ ФЛАГ = TRUE ");
      } else {
        System.out.println("ТЕСТ ФЛАГ = false ");
      }
      System.out.printf("minChance:%1.2f",min_chance_to_shot);

      int sx = game.getLevel().getTiles().length;
      int sy = game.getLevel().getTiles()[0].length;
      System.out.printf("| sx=%3d sy=%3d \n", sx, sy);
      arr = new Tile[sx][sy];
      int unitscount= game.getProperties().getTeamSize()*game.getPlayers().length;
//      System.out.println("unitscount="+unitscount);
      unitscoord = new double[unitscount][unitscoordsize];


      for (int i = 0; i < sx; i++) {
        for (int j = 0; j < sy; j++) {
          arr[i][j] = game.getLevel().getTiles()[i][j];
        }
      }

      if (test_flag) {
        for (int j = 0; j < arr[0].length; j++) {
          for (Tile[] tiles : arr) {
            int prom = 0;
            Tile promTile = tiles[arr[j].length - j - 1];
            if (promTile == Tile.WALL) {
              prom = 1;
            }
            if (promTile == Tile.PLATFORM) {
              prom = 2;
            }
            if (promTile == Tile.LADDER) {
              prom = 3;
            }
            if (promTile == Tile.JUMP_PAD) {
              prom = 4;
            }
            System.out.print(prom);
          }
          System.out.println();
        }
      }
      after_first_tick = true;
    }

    UnitAction action = new UnitAction();
    double distanceToEnemy = 4;
//    System.out.println("Тик:" + game.getCurrentTick());

    long startTickTime = System.nanoTime();

    // поиск ближайшего вражеского юнита
    Unit nearestEnemy = null;
    for (Unit other : game.getUnits()) {
      if (other.getPlayerId() != unit.getPlayerId()) {
        if (nearestEnemy == null || distanceSqr(unit.getPosition(),
                other.getPosition()) < distanceSqr(unit.getPosition(), nearestEnemy.getPosition())) {
          nearestEnemy = other;
        }
      }
    }

    // поиск ближайшего оружия
    LootBox nearestWeapon = null;
    for (LootBox lootBox : game.getLootBoxes()) {
      if (lootBox.getItem() instanceof Item.Weapon) {
        if (nearestWeapon == null || distanceSqr(unit.getPosition(),
                lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestWeapon.getPosition())) {
//          if (((Item.Weapon) lootBox.getItem()).getWeaponType()==WeaponType.ROCKET_LAUNCHER)
          {
            nearestWeapon = lootBox;
          }
        }
      }
    }

    // поиск id
    int mynumb = 0;
    int enemynumb = 0;
    {
      int i = 0;
      for (Player players : game.getPlayers()) {
        if (players.getId() == unit.getPlayerId()) {
          mynumb = i;
        }
        if (players.getId() == nearestEnemy.getPlayerId()) {
          enemynumb = i;
        }
//        System.out.println("CurID"+i);
        i++;
      }
//      System.out.println("MyID"+mynumb + "EnemyID"+enemynumb);
    }


    // поиск ближайшей аптечки
    LootBox nearestHealthPack = null;
    for (LootBox lootBox : game.getLootBoxes()) {
      if (lootBox.getItem() instanceof Item.HealthPack) {
        if (nearestHealthPack == null || distanceSqr(unit.getPosition(),
                lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestHealthPack.getPosition())) {
          nearestHealthPack = lootBox;
        }
      }
    }

    // поиск ближайшей активированной мины
    Mine nearestActiveMine = null;
    for (Mine mine : game.getMines()) {
      if (nearestActiveMine !=null){
        if (distanceSqr(unit.getPosition(), mine.getPosition()) < distanceSqr(unit.getPosition(), nearestActiveMine.getPosition())){
          nearestActiveMine = mine;
        }
      } else {
        nearestActiveMine = mine;
      }
    }
    if (nearestActiveMine == null){
      ActiveMineTimer = 0;
    }
    if (draw_screen) {
      if (nearestActiveMine != null) {
        v1 = new Vec2Float((float) nearestActiveMine.getPosition().getX(), (float) nearestActiveMine.getPosition().getY());
        v2 = new Vec2Float((float) nearestActiveMine.getSize().getX(), (float) nearestActiveMine.getSize().getY());
        v3 = new Vec2Float((float) arr.length / 2, (float) arr[0].length);
        debug.draw(new CustomData.Rect(v1, v2, cl_whitewhite));
        debug.draw(new CustomData.Line(v1, v3, (float) 0.1, cl_white));
      }
    }


    if (unit.getWeapon() != null) {
      if (unit.getWeapon().getMagazine() == 0) {
        action.setReload(true);
//          System.out.println("Перезаряд");
        if (draw_screen) {
          promstr = promstr + " | Перезаряд";
        }
        distanceToEnemy = 10;
        targ  = Targs.RunAway;
      }
    }

    if ((nearestEnemy.getWeapon() != null) && (nearestEnemy.getWeapon().getFireTimer() != null) && (nearestEnemy.getWeapon().getFireTimer()>0.5)){
      distanceToEnemy = 0;
    }

    if (unit.getWeapon() !=null) {
      if (unit.getWeapon().getFireTimer() !=null){
            if (unit.getWeapon().getFireTimer()>0.05){
          distanceToEnemy = 10;
        }
      }
    }



    longtimebetweenshoots = false;

//    promstr = String.format("MyID:%3d EnemyID:%3d",unit.getId(), nearestEnemy.getId());
//    promstr = String.format("MyScore:%3d Enemy:%3d",game.getPlayers()[mynumb].getScore(), game.getPlayers()[enemynumb].getScore());
    v1 = new Vec2Float((float) 1, (float) game.getLevel().getTiles()[0].length - 5);
    debug.draw(new CustomData.PlacedText(promstr, v1, TextAlignment.LEFT, (float) 20, cl_whitewhite));

    if ((unit.getWeapon() != null) && (unit.getWeapon().getLastFireTick() != null)){
      if (print_console) {
        System.out.printf("задержка CurTick: %3d: lastFire: %3d", game.getCurrentTick(), unit.getWeapon().getLastFireTick());
      }
      double timebetweenFire = game.getCurrentTick() - unit.getWeapon().getLastFireTick();
      if (timebetweenFire > unit.getWeapon().getParams().getReloadTime() * 3*game.getProperties().getTicksPerSecond()) {

        double curdistanceX = Math.abs(unit.getPosition().getX() - nearestEnemy.getPosition().getX());
        distanceToEnemy = curdistanceX - timebetweenFire/game.getProperties().getTicksPerSecond()/10;

        if (print_console) {
          System.out.printf("Слишком долго не стреляли CurTick: %3d: lastFire: %3d", game.getCurrentTick(), unit.getWeapon().getLastFireTick());
        }

        if (game.getPlayers()[mynumb].getScore() <= game.getPlayers()[enemynumb].getScore()) {
          longtimebetweenshoots = true;
          distanceToEnemy = 6 - timebetweenFire/game.getProperties().getTicksPerSecond() ;
          if (distanceToEnemy<0) {
            distanceToEnemy = 0;
          }
//          System.out.println(" ПОРА СТРЕЛЯТЬ\n");
        } else {
//          System.out.println(" и не надр.\n");
        }

      }
    }


    // выбор куда бежать если нет оружия, то к ближайщему иначе к ближайшему противнику
    Vec2Double targetPos = unit.getPosition();
    if (unit.getWeapon() == null && nearestWeapon != null) {
      targetPos = nearestWeapon.getPosition();
      targ = Targs.Weapon;

    } else if (nearestEnemy != null) {
      double promX;
      double promY;
      promY = nearestEnemy.getPosition().getY();
      if (nearestEnemy.getPosition().getX()>unit.getPosition().getX()){
        promX = nearestEnemy.getPosition().getX()-distanceToEnemy;
      } else {
        promX = nearestEnemy.getPosition().getX()+distanceToEnemy;
      }
      targetPos = new Vec2Double(promX, promY);
      targ = Targs.NearestEnemy;
    }


    // Если ранение, то к ближайшей аптечке
    if (unit.getHealth() < game.getProperties().getUnitMaxHealth() * woundProcent) {
      if (nearestHealthPack != null) {
        targetPos = nearestHealthPack.getPosition();
        if (test_flag) {
          System.out.println("Аптечка");
        }
        if (draw_screen) {
          debug.draw(new CustomData.Log("Аптечка"));
        }
        targ =Targs.HealthPack;
      }
    }
    // Стараемся не приближаться к установленной мине.
    if (nearestActiveMine != null){
      if (distance_abs(unit.getPosition(), nearestActiveMine.getPosition())<5){
        System.out.printf("гдк то рядом Мина: x=%2.3f y=%2.3f", nearestActiveMine.getPosition().getX(), nearestActiveMine.getPosition().getY());
        if (unit.getPosition().getX() < nearestEnemy.getPosition().getX()){
          double posx = unit.getPosition().getX()-1;
          double posy = unit.getPosition().getY()+5;
          targetPos = new Vec2Double(posx, posy );
          System.out.printf("Минаа: x=%2.3f y=%2.3f", nearestActiveMine.getPosition().getX(), nearestActiveMine.getPosition().getY());
        }
        if (unit.getPosition().getX() >= nearestEnemy.getPosition().getX()){
          double posx = unit.getPosition().getX()+1;
          double posy = unit.getPosition().getY()+5;
          targetPos = new Vec2Double(posx, posy );
          System.out.printf("Минаа: x=%2.3f y=2.3f", nearestActiveMine.getPosition().getX(), nearestActiveMine.getPosition().getY());
        }
      }
    }
    // стараемся уничтожить старую мину
//    (if ((nearestActiveMine != null) && (game.getCurrentTick()-ActiveMineTimer)>3*game.getProperties().getTicksPerSecond()){

//    }

//************** Пробуем ставить мину
    boolean needActiveMine = false;

    if ((unit.getMines() > 0) && (nearestActiveMine == null)) {
      int kx = 1;
      if (unit.getPosition().getX() > nearestEnemy.getPosition().getX()) {
        kx = -1;
      }
        double dist = distance_abs(unit.getPosition(), nearestEnemy.getPosition());
        if ((unit.getPosition().getX() > 10) && (unit.getPosition().getX() < 30)) {
          if ((dist > 5) && (dist < 10)) {
            if ((nearestEnemy.getPosition().getX()>5) && (nearestEnemy.getPosition().getX() < game.getLevel().getTiles().length-5))
            {
              needActiveMine = true;
              System.out.printf("Надо ставить: x=%2.3f y=%2.3f", unit.getPosition().getX(), unit.getPosition().getY());
            }
          }
        }
    }

    // выбор куда целиться
    Vec2Double aim = new Vec2Double(0, 0);
    if (nearestEnemy != null) {
      aim = new Vec2Double(nearestEnemy.getPosition().getX() - unit.getPosition().getX(),
              nearestEnemy.getPosition().getY() - unit.getPosition().getY() + 0.0);
    }

    //************************************************************
    // проверка на возможность попадания в противника
    double enemyX = nearestEnemy.getPosition().getX();
    double enemyY = nearestEnemy.getPosition().getY() + unit.getSize().getY() / 2;
    double unitX = unit.getPosition().getX();
    double unitY = unit.getPosition().getY() + unit.getSize().getY() / 2 - 0.5;

    double deltaX = nearestEnemy.getPosition().getX() - unit.getPosition().getX();
    double deltaY = nearestEnemy.getPosition().getY() - unit.getPosition().getY();
    promstr = "";
    see_enemy = true;

//    System.out.println("Начало проверки");
//System.out.printf("проверка: тик:%5d enemyX=%2.3f enemyY=%2.3f  unitX=%2.3f unitY=%2.3f \n",game.getCurrentTick(), enemyX, enemyY, unitX, unitY);

    if (Math.abs(enemyX - unitX) > Math.abs(enemyY - unitY)){
      if (draw_screen) {
      promstr = "Проверка по X ";
    }
      int kx = 1;
      if (enemyX < unitX) {
        kx = -1;
      }
      if (draw_screen) {
        promstr = promstr + kx;
        debug.draw(new CustomData.Log(promstr));
      }

      double alfa = kx * deltaY / deltaX;

//      int i = 0;
//      boolean bexit = false;
//      while ()
      for (int i = (int) Math.floor(unitX+kx); (Math.abs(enemyX - i) >=1)  ; i = i + kx) {
        double Ypos = unitY + 1.0 * kx * (i - unitX) * alfa;
        int iYpos = (int) Math.floor(Ypos);
/*
if ((game.getCurrentTick()>220) && (game.getCurrentTick()<230)) {
  System.out.printf("проверка: tick=%2d Xint=%2d Yint=%2d Y=%2.4f enemy.y:%2.4f \n",game.getCurrentTick(), i, iYpos, Ypos, enemyY);
    System.out.printf("");
}
*/
        if (draw_screen) {
          v1 = new Vec2Float((float) (0.5), (float) (0 + 0.5));
          v2 = new Vec2Float((float) (i + 0.5), (float) 0.5 + iYpos);
          debug.draw(new CustomData.Line(v1, v2, (float) 0.2, cl_white));
        }

//        System.out.printf("начало проверки: tick=%2d Xint=%2d Yint=%2d Y=%2.3f enemy.x:%2.3f enemy.y:%2.3f \n",game.getCurrentTick(), i, iYpos, Ypos, nearestEnemy.getPosition().getX(),enemyY);
        if (arr[i][iYpos] == Tile.WALL) {
          see_enemy = false;
          if (draw_screen) {
            v1 = new Vec2Float((float) (10.0 + 0.5), (float) (0 + 0.5));
            v2 = new Vec2Float((float) 0.5 + i, (float) 0.5 + iYpos);
            debug.draw(new CustomData.Line(v1, v2, (float) 0.1, cl_red));
            promstr += "Враг не виден. ";
          }
        }

        if (arr[i-1][iYpos] == Tile.WALL) {
          see_enemy = false;
          if (draw_screen)
          {
            v1 = new Vec2Float((float) (10.0 + 0.5), (float) (0 + 0.5));
            v2 = new Vec2Float((float) 0.5 + i-1, (float) 0.5 + iYpos);
            debug.draw(new CustomData.Line(v1, v2, (float) 0.2, cl_redred));
            promstr += "Враг не виден2. ";
//            System.out.printf("проверка: tick=%2d Xint=%2d Yint=%2d Y=%2.4f enemy.y:%2.4f \n",game.getCurrentTick(), i, iYpos, Ypos, enemyY);
          }
        }

      }
    } else {
      if (draw_screen) {
        debug.draw(new CustomData.Log("Проверка по Y"));
      }
      int ky = 1;
      if (enemyY < unitY) {
        if (draw_screen) {
          debug.draw(new CustomData.Log("KY=-1"));
        }
        ky = -1;
      }
      double alfa = ky * deltaX / deltaY;
      for (int i = (int) Math.floor(unitY); Math.abs(enemyY - i) > 0.5; i = i + ky) {
//          double Xpos=Math.round(unit.getPosition().getX()+1.0*ky*(i-unitY)*alfa);
        double Xpos = unit.getPosition().getX() + 1.0 * ky * (i - unitY) * alfa;
        int iXpos = (int) Math.floor(Xpos);

//        System.out.printf("проверка: X=%2.4f Yint=%3d\n",Xpos,i);
        if (draw_screen) {
          v1 = new Vec2Float((float) (0 + 0.5), (float) (0 + 0.5));
          v2 = new Vec2Float((float) 0.5 + iXpos, (float) 0.5 + i);
          debug.draw(new CustomData.Line(v1, v2, (float) 0.1, cl_blue));
        }

        if (arr[iXpos][i] == Tile.WALL) {
          see_enemy = false;
          if (draw_screen)
          {
            v1 = new Vec2Float((float) (10 + 0.5), (float) (0 + 0.5));
            v2 = new Vec2Float((float) 0.5 + iXpos, (float) 0.5 + i);
            debug.draw(new CustomData.Line(v1, v2, (float) 0.1, cl_red));
            promstr += "Враг не виден. ";
          }
        }
        if (arr[iXpos][i+ky] == Tile.WALL) {
          see_enemy = false;
          if (draw_screen)
          {
            v1 = new Vec2Float((float) (10 + 0.5), (float) (0 + 0.5));
            v2 = new Vec2Float((float) 0.5 + iXpos, (float) 0.5 + i+ky);
            debug.draw(new CustomData.Line(v1, v2, (float) 0.2, cl_redred));
            promstr += "Враг не виден2. ";
          }
        }

      }
    }
    debug.draw(new CustomData.Log(promstr));

    // выбор когда прыгать
    boolean jump = false;
    boolean jumpDown = false;
    promstr = "прыжки: targ= " +  targ;
    if (targetPos.getY() > unit.getPosition().getY()) {
      if (targ != Targs.NearestEnemy)  {

//        if (targetPos.getX() > unit.getPosition().getX() && game.getLevel().getTiles()[(int) (unit.getPosition().getX() + 1)][(int) (unit.getPosition().getY())] == Tile.EMPTY) {
        //jump = true;
//        } else {
        jump = true;
        promstr = promstr + " | прыг0";
      }
//        }
    }

    if (targetPos.getY() < unit.getPosition().getY()) {
      if (targetPos.getX() < unit.getPosition().getX() && (game.getLevel().getTiles()[(int) (unit.getPosition().getX() - 1)][(int) (unit.getPosition().getY())] != Tile.WALL)) {
        jumpDown = true;

      }
      if (targetPos.getX() > unit.getPosition().getX() && (game.getLevel().getTiles()[(int) (unit.getPosition().getX() + 1)][(int) (unit.getPosition().getY())] != Tile.WALL)) {
        jumpDown = true;

      }
    }


    if (targetPos.getX() > unit.getPosition().getX() && game.getLevel().getTiles()[(int) (unit.getPosition().getX() + 1)][(int) (unit.getPosition().getY())] == Tile.WALL) {
      jump = true;
      promstr = promstr+" | прыг1";
    }
    if (targetPos.getX() < unit.getPosition().getX() && game.getLevel().getTiles()[(int) (unit.getPosition().getX() - 1)][(int) (unit.getPosition().getY())] == Tile.WALL) {
      jump = true;
      promstr = promstr+" | прыг2";
    }

    if (draw_screen) {
      v1 = new Vec2Float((float) 1, (float) game.getLevel().getTiles()[0].length - 6);
      debug.draw(new CustomData.PlacedText(promstr, v1, TextAlignment.LEFT, (float) 20, cl_whitewhite));
    }

    Tile cur_tile = game.getLevel().getTiles()[(int) (unit.getPosition().getX())][(int) (unit.getPosition().getY() - 0) - 1];

    if ((jumptime > 10) && (cur_tile == Tile.PLATFORM)) {
      jump = false;

    }
    if (jump) {
      jumptime++;
    } else {
      jumptime = 0;
    }

    if (test_flag) {
      if (game.getCurrentTick() < 500) {
        targetPos.setX(39);
       targetPos.setY(0);
       jumpDown = true;
       jump = false;
      }
    }

// движение с макс.скоростью
    double needVelovity = targetPos.getX() - unit.getPosition().getX();
      if (needVelovity >= 0) {
        needVelovity = game.getProperties().getUnitMaxHorizontalSpeed();
      } else {
        needVelovity = -game.getProperties().getUnitMaxHorizontalSpeed();
      }

    if (distance_abs(targetPos, unit.getPosition())<0.5){
      needVelovity = game.getProperties().getUnitMaxHorizontalSpeed()*0.2;

    }
    if (longtimebetweenshoots) {
      needVelovity = game.getProperties().getUnitMaxHorizontalSpeed()*0.5;
    }


    action.setVelocity(needVelovity);
    action.setJump(jump);
    action.setJumpDown(jumpDown);
    action.setAim(aim);



    //********** Подсчет вероятностей  **************************
    double dx1, dx2, dy1,dy2;
    double kx = 1;
    if (unit.getPosition().getX()>nearestEnemy.getPosition().getX()){
      kx =-1;
    }
    if (unit.getPosition().getY() < (nearestEnemy.getPosition().getY()+nearestEnemy.getSize().getY()) ) {
      dx1 = nearestEnemy.getPosition().getX() - kx*nearestEnemy.getSize().getX() / 2 - unit.getPosition().getX();
    } else {
      dx1 = nearestEnemy.getPosition().getX() + kx*nearestEnemy.getSize().getX() / 2 - unit.getPosition().getX();
    }

    if (unit.getPosition().getY()<nearestEnemy.getPosition().getY()) {
      dx2 = nearestEnemy.getPosition().getX() + kx*nearestEnemy.getSize().getX() / 2 - unit.getPosition().getX();
    } else {
      dx2 = nearestEnemy.getPosition().getX() - kx*nearestEnemy.getSize().getX() / 2 - unit.getPosition().getX();
    }

    dy1 = nearestEnemy.getPosition().getY()+nearestEnemy.getSize().getY() - unit.getPosition().getY();
    dy2 = nearestEnemy.getPosition().getY() - unit.getPosition().getY();



    double alfa1 = 90*Math.PI/180;
    if (dx1 != 1) {
      alfa1 = Math.atan(dy1 / dx1);
    }
    double alfa2 = 90*Math.PI/180;
    if (dx2 != 1) {
      alfa2 = Math.atan(dy2 / dx2);
    }
    double chance;

    if (draw_screen) {
      v1 = new Vec2Float((float) unit.getPosition().getX(), (float) unit.getPosition().getY());
      v2 = new Vec2Float((float) (dx1 + unit.getPosition().getX()), (float) (dy1 + unit.getPosition().getY()));
      debug.draw(new CustomData.Line(v1, v2, (float) 0.1, cl_white));
      v2 = new Vec2Float((float) (dx2 + unit.getPosition().getX()), (float) (dy2 + unit.getPosition().getY()));
      debug.draw(new CustomData.Line(v1, v2, (float) 0.1, cl_white));
    }

    if (unit.getWeapon() !=null) {

      if (unit.getWeapon().getSpread() != 0){
        if (print_console) {
//          System.out.println(String.format("Такт:%2d alfa1:%2.3f alfa2:%2.3f spread:%2.3f", game.getCurrentTick(), alfa1 * 180 / Math.PI, alfa2 * 180 / Math.PI, 360 / Math.PI * unit.getWeapon().getSpread()));
        }
        chance = Math.abs((alfa1 - alfa2) / (2*unit.getWeapon().getSpread()));
      }
     else {
      chance = 1;
    }
    }
    else {
      chance = 0;
    }



    //********** Предсказание траетории протиника  **************************
    if (draw_screen) {
      promstr = "";
      promstr += "";
    }
//    game.getUnits()[0]

    //********** Минирование
    //********** Уход от пуль

    // Отображение разброса
    if (draw_screen) {
    if (unit.getWeapon() != null) {
      double cur_spread = unit.getWeapon().getSpread();
      double cur_aimangle = 0;
      if (aim.getX() != 0) {
        cur_aimangle = Math.atan(aim.getY() / aim.getX());
      } else {
        if (aim.getY() > 0) {
          cur_aimangle = 90.0 * Math.PI / 180;
        } else {
          cur_aimangle = 90.0 * Math.PI / 180;
        }
      }
      double distance = Math.sqrt(aim.getX() * aim.getX() + aim.getY() * aim.getY());
      alfa1 = cur_aimangle + cur_spread;
      double promX1;
      double promY1;
      promX1 = Math.cos(alfa1) * distance;
      promY1 = Math.sin(alfa1) * distance;

      alfa2 = cur_aimangle - cur_spread;
      double promX2;
      double promY2;
      promX2 = Math.cos(alfa2) * distance;
      promY2 = Math.sin(alfa2) * distance;

      debug.draw(new CustomData.Log(String.format("aim.X=%2.2f;aim.Y=%2.2f Cur.Angle: %2.2f Alfa1:%2.2f dist:%2.3f disttoenemyX:%2.2f promX=%2.2f promY=%2.2f",
              aim.getX(), aim.getY(), ingradus(cur_aimangle), ingradus(alfa1), distance, distanceToEnemy, promX1, promY1)));

      v2 = new Vec2Float((float) unit.getPosition().getX(), (float) (unit.getPosition().getY() + unit.getSize().getY() / 2));
      v3 = new Vec2Float((float) (promX1 + unit.getPosition().getX()), (float) (promY1 + unit.getPosition().getY() + unit.getSize().getY() / 2));
      debug.draw(new CustomData.Line(v2, v3, (float) 0.1, cl_green));
      v3 = new Vec2Float((float) (promX2 + unit.getPosition().getX()), (float) (promY2 + unit.getPosition().getY() + unit.getSize().getY() / 2));
      debug.draw(new CustomData.Line(v2, v3, (float) 0.1, cl_green));
    }
    }




      //***************************
      // Виден ли противник
    promstr = "";
      boolean needShoot = false;
      if ((unit.getWeapon() != null) && (see_enemy)) {
        if (draw_screen) {
          promstr = promstr + " | Вижу цель";
        }
        if ((chance > min_chance_to_shot) || (unit.getWeapon().getSpread() == unit.getWeapon().getParams().getMinSpread())) {
          needShoot = true;
        }
      }

    if (test_flag){
      needShoot = false;
    }

    if (game.getCurrentTick() == 700){
      needShoot = true;
    }
/*
    if (game.getCurrentTick() == 1200){
      needShoot = true;
    }
*/
      action.setShoot(needShoot);

      //***************************

//    action.setSwapWeapon(false);
      action.setPlantMine(needActiveMine);







      // подбор наилучшего оружия пистолет - гранатомет - винтовка
    action.setSwapWeapon(false);
    if (unit.getWeapon()==null) {
      action.setSwapWeapon(true);
    } else {
/*
          if (nearestWeapon != null && distanceSqr(unit.getPosition(), nearestWeapon.getPosition()) < 1) {
            promstr = promstr+ " | оружие под ногами";
            if (((Item.Weapon) nearestWeapon.getItem()).getWeaponType()==WeaponType.ASSAULT_RIFLE){
              if (unit.getWeapon().getTyp() != WeaponType.ASSAULT_RIFLE) {
                action.setSwapWeapon(true);
                promstr = promstr+ " | подбираем винтовку";
              }
            }
            if (((Item.Weapon) nearestWeapon.getItem()).getWeaponType()==WeaponType.ROCKET_LAUNCHER) {
              if (unit.getWeapon().getTyp() == WeaponType.PISTOL) {
                promstr = promstr + "| меняем пистолет на гранатомет";
              }
            }
          }
 */
      if (nearestWeapon != null && distanceSqr(unit.getPosition(), nearestWeapon.getPosition()) < 1) {
        promstr = promstr+ " | оружие под ногами";
        if (((Item.Weapon) nearestWeapon.getItem()).getWeaponType()==WeaponType.PISTOL){
          if (unit.getWeapon().getTyp() != WeaponType.PISTOL) {
            action.setSwapWeapon(true);
            promstr = promstr+ " | подбираем пистолет";
          }
        }
        if (((Item.Weapon) nearestWeapon.getItem()).getWeaponType()==WeaponType.ROCKET_LAUNCHER) {
          if (unit.getWeapon().getTyp() == WeaponType.ASSAULT_RIFLE) {
            action.setSwapWeapon(true);
            promstr = promstr + "| меняем винтовку на гранатомет";
          }
        }
      }
    }
    if (draw_screen) {
      v1 = new Vec2Float((float) 1, (float) game.getLevel().getTiles()[0].length - 7);
      debug.draw(new CustomData.PlacedText(promstr, v1, TextAlignment.LEFT, (float) 20, cl_whitewhite));
    }

//***********************************************
      long time_cur_tick = System.nanoTime() - startTickTime;
//    System.out.println("tick:"+game.getCurrentTick()+" tima:"+System.nanoTime()+" delta:" + time_cur_tick+" time_longest_tick:"+time_longest_tick);
      if (game.getCurrentTick()<=2) {
//        System.out.printf("cur_tick: %3d %1.3f мсек \n", game.getCurrentTick(), (float) time_cur_tick / 1000000);
      }
      if ((time_longest_tick < time_cur_tick) && (game.getCurrentTick() > 2)) {
        time_longest_tick = time_cur_tick;
        longest_tick = game.getCurrentTick();
        System.out.printf("long_tick:%3d %1.3f мсек \n", longest_tick, (float)time_longest_tick/1000000);
      }

//печать на экране
    //  grid
    if (draw_screen) {
      for (int i = 0; i < arr.length + 1; i++) {
        v1 = new Vec2Float((float) i, (float) 0);
        v2 = new Vec2Float((float) i, (float) arr[0].length);
        debug.draw(new CustomData.Line(v1, v2, (float) 0.05, cl_white));
        debug.draw(new CustomData.PlacedText(String.format("%2d", i), v1, TextAlignment.LEFT, (float) 20, cl_whitewhite));
      }
      for (int j = 0; j < arr[0].length + 1; j++) {
        v1 = new Vec2Float((float) 0, (float) j);
        v2 = new Vec2Float((float) arr.length, (float) j);
        debug.draw(new CustomData.Line(v1, v2, (float) 0.05, cl_white));
        debug.draw(new CustomData.PlacedText(String.format("%2d", j), v1, TextAlignment.LEFT, (float) 20, cl_whitewhite));
      }
    }

    if (draw_screen) {
      promstr = String.format("Tick=%3d| Unit:X=%3.2f Y=%3.2f Health:%3d| Chance:%1.3f ", game.getCurrentTick(), unit.getPosition().getX(), unit.getPosition().getY(), unit.getHealth(), chance);

      if (unit.getWeapon() != null) {
        promstr = promstr + String.format("Bullets:%2d Spread %2.3f..%2.3f cur:%2.3f firetimer=%2.3f", unit.getWeapon().getMagazine(),
                unit.getWeapon().getParams().getMinSpread(), unit.getWeapon().getParams().getMaxSpread(), unit.getWeapon().getSpread(), unit.getWeapon().getFireTimer());
      } else {
        promstr = promstr + "Нет оружия. ";
      }
      v1 = new Vec2Float((float) 1, (float) game.getLevel().getTiles()[0].length - 1);
      debug.draw(new CustomData.PlacedText(promstr, v1, TextAlignment.LEFT, (float) 20, cl_whitewhite));

      promstr = String.format("| TargetPos: X=%2.3f Y=%2.3f Health:%3d ",
              targetPos.getX(), targetPos.getY(), nearestEnemy.getHealth());
      if (nearestEnemy.getWeapon() != null) {
        promstr = promstr + String.format("|fire_timer:%2.3f bullets:%2d mines:%d JUMPSTATE MaxTime:%2.3f speed:%2.3f cancancel:%b canjump:%b ",
                nearestEnemy.getWeapon().getFireTimer(), nearestEnemy.getWeapon().getMagazine(), nearestEnemy.getMines(),
                nearestEnemy.getJumpState().getMaxTime(), nearestEnemy.getJumpState().getSpeed(), nearestEnemy.getJumpState().isCanCancel(), nearestEnemy.getJumpState().isCanJump());
      }

      if (nearestHealthPack != null) {
        promstr = promstr + String.format("| HealthPack: X=%2.3f Y=%2.3f", nearestHealthPack.getPosition().getY(), nearestHealthPack.getPosition().getY());
      } else {
        promstr = promstr + " | Нет аптечки";
      }
      if (action.isJump()) {
        promstr = promstr + " | ПРЫЖОК";
      } else {
        promstr = promstr + " | нет прыжка вверх";
      }
      if (action.isJumpDown()) {
        promstr = promstr + " | ВНИЗ";
      } else {
        promstr = promstr + " | нет прыжка вниз";
      }

//    debug.draw(new CustomData.Log(promstr));
      v1 = new Vec2Float((float) 1, (float) game.getLevel().getTiles()[0].length - 2);
      debug.draw(new CustomData.PlacedText(promstr, v1, TextAlignment.LEFT, (float) 20, cl_whitewhite));

      promstr = String.format("MyScore:%3d Enemy:%3d",game.getPlayers()[mynumb].getScore(), game.getPlayers()[enemynumb].getScore());
      v1 = new Vec2Float((float) 1, (float) game.getLevel().getTiles()[0].length - 5);
      debug.draw(new CustomData.PlacedText(promstr, v1, TextAlignment.LEFT, (float) 20, cl_whitewhite));

/*
      promstr = String.format("Pistol :| Magazine:%3d| AimSpeed:%2.3f| FireRate:%2.3f| ReloadT:%2.3f| MinSpread:%2.3f| MaxSpread:%2.3f| Recoil:%2.3f|  BULLET:| Damage: %2d| Vel: %2.3f Size: %2.3f",
              game.getProperties().getWeaponParams().get(WeaponType.PISTOL).getMagazineSize(), game.getProperties().getWeaponParams().get(WeaponType.PISTOL).getAimSpeed(),
              game.getProperties().getWeaponParams().get(WeaponType.PISTOL).getFireRate(), game.getProperties().getWeaponParams().get(WeaponType.PISTOL).getReloadTime(),
              game.getProperties().getWeaponParams().get(WeaponType.PISTOL).getMinSpread(), game.getProperties().getWeaponParams().get(WeaponType.PISTOL).getMaxSpread(),
              game.getProperties().getWeaponParams().get(WeaponType.PISTOL).getRecoil(), game.getProperties().getWeaponParams().get(WeaponType.PISTOL).getBullet().getDamage(),
              game.getProperties().getWeaponParams().get(WeaponType.PISTOL).getBullet().getSpeed(), game.getProperties().getWeaponParams().get(WeaponType.PISTOL).getBullet().getSize());
      v1 = new Vec2Float((float) 1, (float) game.getLevel().getTiles()[0].length - 3);
      debug.draw(new CustomData.PlacedText(promstr, v1, TextAlignment.LEFT, (float) 20, cl_whitewhite));

      promstr = String.format("Rifle  :| Magazine:%3d| AimSpeed:%2.3f| FireRate:%2.3f| ReloadT:%2.3f| MinSpread:%2.3f| MaxSpread:%2.3f Recoil:%2.3f|  BULLET:| Damage: %2d| Vel: %2.3f Size: %2.3f",
              game.getProperties().getWeaponParams().get(WeaponType.ASSAULT_RIFLE).getMagazineSize(), game.getProperties().getWeaponParams().get(WeaponType.ASSAULT_RIFLE).getAimSpeed(),
              game.getProperties().getWeaponParams().get(WeaponType.ASSAULT_RIFLE).getFireRate(), game.getProperties().getWeaponParams().get(WeaponType.ASSAULT_RIFLE).getReloadTime(),
              game.getProperties().getWeaponParams().get(WeaponType.ASSAULT_RIFLE).getMinSpread(), game.getProperties().getWeaponParams().get(WeaponType.ASSAULT_RIFLE).getMaxSpread(),
              game.getProperties().getWeaponParams().get(WeaponType.ASSAULT_RIFLE).getRecoil(), game.getProperties().getWeaponParams().get(WeaponType.ASSAULT_RIFLE).getBullet().getDamage(),
              game.getProperties().getWeaponParams().get(WeaponType.ASSAULT_RIFLE).getBullet().getSpeed(), game.getProperties().getWeaponParams().get(WeaponType.ASSAULT_RIFLE).getBullet().getSize());

      v1 = new Vec2Float((float) 1, (float) game.getLevel().getTiles()[0].length - 4);
      debug.draw(new CustomData.PlacedText(promstr, v1, TextAlignment.LEFT, (float) 20, cl_whitewhite));
      promstr = String.format("Launcer:| Magazine:%3d| AimSpeed:%2.3f| FireRate:%2.3f| ReloadT:%2.3f| MinSpread:%2.3f| MaxSpread:%2.3f Recoil:%2.3f|  BULLET:| Damage: %2d| Vel: %2.3f Size: %2.3f",
              game.getProperties().getWeaponParams().get(WeaponType.ROCKET_LAUNCHER).getMagazineSize(), game.getProperties().getWeaponParams().get(WeaponType.ROCKET_LAUNCHER).getAimSpeed(),
              game.getProperties().getWeaponParams().get(WeaponType.ROCKET_LAUNCHER).getFireRate(), game.getProperties().getWeaponParams().get(WeaponType.ROCKET_LAUNCHER).getReloadTime(),
              game.getProperties().getWeaponParams().get(WeaponType.ROCKET_LAUNCHER).getMinSpread(), game.getProperties().getWeaponParams().get(WeaponType.ROCKET_LAUNCHER).getMaxSpread(),
              game.getProperties().getWeaponParams().get(WeaponType.ROCKET_LAUNCHER).getRecoil(), game.getProperties().getWeaponParams().get(WeaponType.ROCKET_LAUNCHER).getBullet().getDamage(),
              game.getProperties().getWeaponParams().get(WeaponType.ROCKET_LAUNCHER).getBullet().getSpeed(), game.getProperties().getWeaponParams().get(WeaponType.ROCKET_LAUNCHER).getBullet().getSize());
      v1 = new Vec2Float((float) 1, (float) game.getLevel().getTiles()[0].length - 5);
      debug.draw(new CustomData.PlacedText(promstr, v1, TextAlignment.LEFT, (float) 20, cl_whitewhite));
*/

      // массив пуль
      for (Bullet bullet : game.getBullets()) {
        double posx = bullet.getPosition().getX();
        double posy = bullet.getPosition().getY();
        v1 = new Vec2Float((float) bullet.getPosition().getX(), (float) bullet.getPosition().getY());
        v2 = new Vec2Float((float) bullet.getSize(), (float) bullet.getSize());
        v3 = new Vec2Float((float) arr.length / 2, (float) arr[0].length);
        debug.draw(new CustomData.Rect(v1, v2, cl_red));
        debug.draw(new CustomData.Line(v1, v3, (float) 0.1, cl_white));
      }

      v1 = new Vec2Float((float) targetPos.getX(), (float) targetPos.getY());
      v2 = new Vec2Float((float) unit.getPosition().getX(), (float) (unit.getPosition().getY() + unit.getSize().getY() / 2));
      v3 = new Vec2Float((float) (aim.getX() + unit.getPosition().getX()), (float) (aim.getY() + unit.getPosition().getY() + unit.getSize().getY() / 2));
//      promstr = String.format("Aim X=%2.3f Y=%2.3f",aim.getX(),aim.getY());
//      debug.draw(new CustomData.Log(promstr));

      debug.draw(new CustomData.Line(v2, v1, (float) 0.1, cl_red));
      debug.draw(new CustomData.Line(v2, v3, (float) 0.1, cl_green));

//      double (aim.getX() + unit.getPosition().getX());
//      double spread= unit.getWeapon().getSpread();

    }
//***********************************************

      return action;
    }

}