import com.google.gson.Gson;
import model.*;
import model.Robot;
public final class MyStrategy implements Strategy {

    static class CalculPoint {  // точка и время
        int tick;
        double x;
        double y;
        double z;
    }

    static class Pos {
        double x;
        double y;
        double z;
    }

    static class Vel {
        double vx;
        double vy;
        double vz;
    }

    static class Acc {
        double ax;
        double ay;
        double az;
    }

    static class Normal {
    }

    static class Object {
        Pos pos;
        Vel vel;
        Acc acc;
        Normal norm;
        boolean touch;
        double radius;
        double alfa;

    }

    static class Robot2 {
        Robot rob;
        Action act;
        int count;
    }

    static class Dist_Norm {
        Pos distance;
        Pos normal;
        Pos pos;
    }
    boolean initial = false;
    int counter = 0;
    int counter2 = 0;
    int team_size;
    int MAX_ROBOTS = 10;
    int PREDICT_POINTS = 60;
    int DEFEND_PREDICT_POINTS = 40; //кол-во точек для защиты
    int MAX_TEAM_SIZE = 5;
    int MAX_OFFSET_FOR_DEFENCE = 0;
    double DELTA_RADIUS_ATTACK = 5;
    int defender = 0;
    int act_tick = 0;
    long startfulltime = 0;
    long sumtime = 0;
    long time_longest_tick = 0;
    long longest_tick = 0;
    long full_attack_time = 0;
    boolean bool_time_tick = false;
    boolean collide_with_object = false;

    String VERSION = "v.2.55  12.01.19";
    String strToScr;
    String strToScr2;
    String graphsToScr;
    String[] prevstrings = new String[MAX_TEAM_SIZE * 2];
    String[] graph_current_string = new String[MAX_TEAM_SIZE * 2];
    Robot[] robots_arr = new Robot[MAX_ROBOTS];
    Object[] ball_arr = new Object[PREDICT_POINTS];
    Object[] ball_arr_micro = new Object[100];
    Object[] rob_arr = new Object[PREDICT_POINTS];


    //boolean DEBUG_ON = true;
    boolean DEBUG_ON = false; // печать в консоль
    boolean DEB_ON_SCR = false; //печать на экране
    boolean GRAPH_ON_SCR = true; //печать на экране графики
    int ticks_per_round = 0;
    CalculPoint TimeBallInGate(Robot me, Rules rules, Game game) {

        double GATE_LINE = -rules.arena.depth / 2 + me.radius + rules.arena.bottom_radius;
        CalculPoint calculpoint = new CalculPoint();
        calculpoint.z = GATE_LINE;
        boolean bperes = false;
        int i = 1;
        if (DEB_ON_SCR) {
            strToScr = strToScr + " TimeBallInGate";
        }
        if (ball_arr[0].vel.vz < 0) { // если мяч катится в сторону ворот
            if (DEB_ON_SCR) {
                strToScr = strToScr + String.format(" мяч в сторону ворот (GATELINE=%5.2f)", GATE_LINE);
            }
            if (game.ball.z < GATE_LINE) {
                bperes = true;
                if (DEB_ON_SCR) {
                    strToScr = strToScr + " мяч уже внутри )";
                }
            }
            while ((!bperes) && (i < PREDICT_POINTS)) {
                if (((ball_arr[i].pos.z <= GATE_LINE) && (ball_arr[i - 1].pos.z > GATE_LINE)) &&
                        (Math.abs(ball_arr[i].pos.x) < rules.arena.goal_width / 2)) {
                    bperes = true;
                }
                i++;
            }
        }
        if (bperes) { // если обнар. пересечение ворот
            if (DEB_ON_SCR) {
                strToScr = strToScr + " есть пересеч.";
            }
            double d_b_gl = delta(GATE_LINE, ball_arr[i].pos.z);
            double d_stp_x = delta(ball_arr[i].pos.x, ball_arr[i - 1].pos.x);
            double d_stp_z = delta(ball_arr[i].pos.z, ball_arr[i - 1].pos.z);
            if (d_stp_z == 0) {
                calculpoint.x = ball_arr[i - 1].pos.x - d_stp_x * d_b_gl / d_stp_z;
            } else {
                calculpoint.x = ball_arr[i].pos.x;
            }

            if (ball_arr[i].pos.y < rules.arena.goal_height) {
                calculpoint.tick = i - 1;
            } else { //если выше ворот
                calculpoint.tick = -1;
            }
        } else {
            calculpoint.tick = -2; //если нет пересечения
        }
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format("Пересеч. через: %d тактов", calculpoint.tick);
        }
        return calculpoint;
    }

    /***********************************************/
    Robot2 moveToPoint(Robot me, Rules rules, Pos pos, int tacts) { // Движение в точку с заданными координатами за заданное время.
        if (DEBUG_ON) {
            System.out.printf("\n***mtp: x:%6.2f z:%6.2f", pos.x, pos.z);
        }


        Robot2 next_step = new Robot2(); // структура хранения результата
        next_step.rob = new Robot();
        next_step.act = new Action();


        double dx = delta(me.x, pos.x);
        double dz = delta(me.z, pos.z);

        double s = Math.sqrt(dx * dx + dz * dz);
        if (DEBUG_ON) {
            System.out.printf(" s:%6.2f", s);
        }
        double alfa = 0;
        if (dz != 0) {
            alfa = Math.atan(-dx / dz);
        }
        double beta;
        if (me.velocity_z != 0) {
            beta = Math.atan(me.velocity_x / me.velocity_z);
        }

        double v0 = Math.sqrt(me.velocity_x * me.velocity_x + me.velocity_z * me.velocity_z);
        if (DEBUG_ON) {
            System.out.printf(" v0:%6.2f", v0);
        }
        double v0s = me.velocity_z * Math.cos(alfa); // проекция на ось направления
        v0s = Math.abs(v0s);
        if (DEBUG_ON) {
            System.out.printf(" Alfa:%6.2f", Math.toDegrees(alfa));
        }

        double t_to_vzero = v0 / rules.ROBOT_ACCELERATION; //время снижения скорости до нуля
        if (DEBUG_ON) {
            System.out.printf(" t_to_vzero:%6.2f", t_to_vzero);
        }
        double t_to_vmax = (rules.ROBOT_MAX_GROUND_SPEED - v0) / rules.ROBOT_ACCELERATION; //время роста скорости до максимума
        if (DEBUG_ON) {
            System.out.printf(" t_to_vmax:%6.2f", t_to_vmax);
        }
        double s_to_zero = v0 * t_to_vzero + rules.ROBOT_MAX_GROUND_SPEED * t_to_vzero * t_to_vzero;
        if (DEBUG_ON) {
            System.out.printf(" s_to_zero:%6.2f", s_to_zero);
        }
        double s_to_max = v0s * t_to_vmax + rules.ROBOT_MAX_GROUND_SPEED * t_to_vmax * t_to_vmax / 2;
        if (DEBUG_ON) {
            System.out.printf(" s_to_max:%6.2f", s_to_max);
            System.out.print(" 9999");
        }
        if (tacts >= 0) {
            if (s_to_zero > s) { // начинать резко тормозить
                if (DEBUG_ON) {
                    System.out.print(" 0000");
                }
                next_step.act.target_velocity_z = 0;
                next_step.act.target_velocity_x = 0;
            } else if (2 * s_to_max + s_to_zero > s) { // если не успеваем разогнаться
                if (DEBUG_ON) {
                    System.out.print(" 1111");
                }
                if (dz >= 0) {
                    next_step.act.target_velocity_z = rules.ROBOT_MAX_GROUND_SPEED * Math.cos(alfa);
                    next_step.act.target_velocity_x = -rules.ROBOT_MAX_GROUND_SPEED * Math.sin(alfa);
                } else {
                    next_step.act.target_velocity_z = -rules.ROBOT_MAX_GROUND_SPEED * Math.cos(alfa);
                    next_step.act.target_velocity_x = rules.ROBOT_MAX_GROUND_SPEED * Math.sin(alfa);
                }
            } else { // достаточно времени для полного разгона и останова
                if (DEBUG_ON) {
                    System.out.print(" 2222");
                }
                if (dz >= 0) {
                    next_step.act.target_velocity_z = rules.ROBOT_MAX_GROUND_SPEED * Math.cos(alfa);
                    next_step.act.target_velocity_x = -rules.ROBOT_MAX_GROUND_SPEED * Math.sin(alfa);
                } else {
                    next_step.act.target_velocity_z = -rules.ROBOT_MAX_GROUND_SPEED * Math.cos(alfa);
                    next_step.act.target_velocity_x = rules.ROBOT_MAX_GROUND_SPEED * Math.sin(alfa);
                }
            }
        } else { // если i = -1, двиuаться в нужную точку с макс. скоростью
            if (DEBUG_ON) {
                System.out.print(" 3333");
            }
            if (dz >= 0) {
                if (DEBUG_ON) {
                    System.out.print(" 4444");
                }
                next_step.act.target_velocity_z = rules.ROBOT_MAX_GROUND_SPEED * Math.cos(alfa);
                next_step.act.target_velocity_x = -rules.ROBOT_MAX_GROUND_SPEED * Math.sin(alfa);
            } else {
                if (DEBUG_ON) {
                    System.out.print(" 5555");
                }
                next_step.act.target_velocity_z = -rules.ROBOT_MAX_GROUND_SPEED * Math.cos(alfa);
                next_step.act.target_velocity_x = rules.ROBOT_MAX_GROUND_SPEED * Math.sin(alfa);
            }
        }

        next_step.rob.x = me.x + me.velocity_x / rules.TICKS_PER_SECOND;
        next_step.rob.y = me.y + me.velocity_y / rules.TICKS_PER_SECOND;
        if (next_step.rob.y < me.radius) {
            next_step.rob.y = me.radius;
        }
        next_step.rob.z = me.z + me.velocity_z / rules.TICKS_PER_SECOND;
        if (DEBUG_ON) {
            System.out.printf(" me_velx:%6.2f me_velz:%6.2f ", me.velocity_x, me.velocity_z);
            System.out.printf(" ns_velx:%6.2f ns_velz:%6.2f ***mtp \n", next_step.act.target_velocity_x, next_step.act.target_velocity_z);
        }
        return next_step;
    }

    /***********************************************/
    void Initial(Robot me, Rules rules, Game game, Action action) {
        for (int j = 0; j < PREDICT_POINTS; j++) {
            ball_arr[j] = new Object();
            ball_arr[j].pos = new Pos();
            ball_arr[j].vel = new Vel();
            ball_arr[j].acc = new Acc();
            ball_arr[j].norm = new Normal();
        }
        for (int j = 0; j < rules.MICROTICKS_PER_TICK; j++) {
            ball_arr_micro[j] = new Object();
            ball_arr_micro[j].pos = new Pos();
            ball_arr_micro[j].vel = new Vel();
            ball_arr_micro[j].acc = new Acc();
            ball_arr_micro[j].norm = new Normal();
        }

        for (int j = 0; j < PREDICT_POINTS; j++) {
            rob_arr[j] = new Object();
            rob_arr[j].pos = new Pos();
            rob_arr[j].vel = new Vel();
            rob_arr[j].acc = new Acc();
            rob_arr[j].norm = new Normal();
        }

        for (int j = 0; j < rules.team_size * 2; j++) {
            robots_arr[j] = me;
        }
        for (int j = 0; j < rules.team_size * 2; j++) {
            prevstrings[j] = String.format("%d", j);
        }
    }

    //****************************************
    Dist_Norm minimum(Dist_Norm dn1, Dist_Norm dn2) {
//System.out.println("*minimum");
//        System.out.println(String.format("111._____dn1_pos_x:%5.2f y:%5.2f z:%5.2f _____dn1_dist_dx:%5.2f dy:%5.2f dz:%5.2f _____dn1_normal_nx:%5.2f ny:%5.2f nz:%5.2f",
//                dn1.pos.x, dn1.pos.y, dn1.pos.z, dn1.distance.x, dn1.distance.y, dn1.distance.z, dn1.normal.x, dn1.normal.y, dn1.normal.z));
//        System.out.println(String.format("222._____dn2_pos_x:%5.2f y:%5.2f z:%5.2f _____dn2_dist_dx:%5.2f dy:%5.2f dz:%5.2f _____dn2_normal_nx:%5.2f ny:%5.2f nz:%5.2f",
//                dn2.pos.x, dn2.pos.y, dn2.pos.z, dn2.distance.x, dn2.distance.y, dn2.distance.z, dn2.normal.x, dn2.normal.y, dn2.normal.z));

        Dist_Norm dn = new Dist_Norm();
        dn.distance = new Pos();
        dn.pos = new Pos();
        dn.normal = new Pos();
        dn.pos.x = dn1.pos.x;
        dn.pos.y = dn1.pos.y;
        dn.pos.z = dn1.pos.z;
//        double dist1 = dn1.distance.x * dn1.distance.x + dn1.distance.y * dn1.distance.y + dn1.distance.z * dn1.distance.z;
//        double dist2 = dn2.distance.x * dn2.distance.x + dn2.distance.y * dn2.distance.y + dn2.distance.z * dn2.distance.z;
        if ((dn1.distance.x < dn2.distance.x) && (dn1.distance.x > 0) || (dn2.distance.x == 0)) {
            dn.distance.x = dn1.distance.x;
            dn.normal.x = dn1.normal.x;
        } else {
            dn.distance.x = dn2.distance.x;
            dn.normal.x = dn2.normal.x;
        }
        if ((dn1.distance.y < dn2.distance.y) && (dn1.distance.y > 0) || (dn2.distance.y == 0)) {
            dn.distance.y = dn1.distance.y;
            dn.normal.y = dn1.normal.y;
        } else {
            dn.distance.y = dn2.distance.y;
            dn.normal.y = dn2.normal.y;
        }
        if ((dn1.distance.z < dn2.distance.z) && (dn1.distance.z > 0) || (dn2.distance.z == 0)) {
            dn.distance.z = dn1.distance.z;
            dn.normal.z = dn1.normal.z;
        } else {
            dn.distance.z = dn2.distance.z;
            dn.normal.z = dn2.normal.z;
        }
//System.out.println(String.format("333._____dn_pos_x:%5.2f y:%5.2f z:%5.2f _____dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f _____dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f",
        //               dn.pos.x, dn.pos.y, dn.pos.z, dn.distance.x, dn.distance.y, dn.distance.z, dn.normal.x, dn.normal.y, dn.normal.z));

//System.out.println("minimum*");
        return dn;
    }

    /***********************************************/
    Dist_Norm dan_to_plane(Dist_Norm dn, double[] point_on_plane, double[] plane_normal) {
        Dist_Norm prom_dn = new Dist_Norm();
        prom_dn.distance = new Pos();
        prom_dn.normal = new Pos();
        prom_dn.pos = new Pos();

        prom_dn.distance.x = Math.abs(delta(point_on_plane[0], dn.pos.x) * plane_normal[0]);
        prom_dn.distance.y = Math.abs(delta(point_on_plane[1], dn.pos.y) * plane_normal[1]);
        prom_dn.distance.z = Math.abs(delta(point_on_plane[2], dn.pos.z) * plane_normal[2]);
        prom_dn.normal.x = plane_normal[0];
        prom_dn.normal.y = plane_normal[1];
        prom_dn.normal.z = plane_normal[2];
        prom_dn.pos.x = dn.pos.x;
        prom_dn.pos.y = dn.pos.y;
        prom_dn.pos.z = dn.pos.z;
        return prom_dn;
    }

    /***********************************************/
    Dist_Norm dan_to_sphere_inner(Dist_Norm dn, double[] sphere_center, double sphere_radius, double ARENA_E) {
        System.out.println("*dan_tosphere_inner");
        System.out.printf("1111.__dn_pos_x:%7.4f y:%7.4f z:%7.4f%n", dn.pos.x, dn.pos.y, dn.pos.z);
        System.out.printf("2222.sph_center:%7.4f y:%7.4f z:%7.4f%n", sphere_center[0], sphere_center[1], sphere_center[2]);

        Dist_Norm prom_dn = new Dist_Norm();
        prom_dn.distance = new Pos();
        prom_dn.normal = new Pos();
        prom_dn.pos = new Pos();

        double dx = delta(dn.pos.x, sphere_center[0]);
        double dy = delta(dn.pos.y, sphere_center[1]);
        double dz = delta(dn.pos.z, sphere_center[2]);

        System.out.printf("3333.dx:%7.4f dy:%7.4f dz:%7.4f %n", dx, dy, dz);

        double d_xz = Math.sqrt(dx * dx + dz * dz);
        double beta = Math.toRadians(90);
        if (d_xz != 0) {
            beta = Math.atan(dy / d_xz);
        }
        double ny = 1 * Math.sin(beta);
        double n_xz = 1 * Math.cos(beta);
        double alfa = Math.toRadians(90);
        if (dz != 0) {
            alfa = Math.atan(dx / dz);
        }
        double nx = n_xz * Math.sin(alfa);
        double nz = n_xz * Math.cos(alfa);

        prom_dn.normal.x = -nx;
        prom_dn.normal.y = ny;
        prom_dn.normal.z = nz;

        prom_dn.pos.x = dn.pos.x;
        prom_dn.pos.y = dn.pos.y;
        prom_dn.pos.z = dn.pos.z;

        double dist_x = delta(sphere_center[0], dn.pos.x);
        double dist_y = delta(sphere_center[1], dn.pos.y);
        double dist_z = delta(sphere_center[2], dn.pos.z);

        double dist = Math.sqrt(dist_x * dist_x + dist_y * dist_y + dist_z * dist_z);
        prom_dn.distance.x = 0;
        prom_dn.distance.y = Math.abs(sphere_radius - dist);
        prom_dn.distance.z = 0;

//        prom_dn.distance.x = Math.abs(sphere_radius*Math.cos(beta)-Math.abs(dn.pos.x-sphere_center[0]));
//        prom_dn.distance.y = Math.abs(sphere_radius*Math.sin(beta)-Math.abs(dn.pos.y-sphere_center[1]));
//        prom_dn.distance.z = Math.abs(sphere_radius*Math.cos(alfa)-Math.abs(dn.pos.z-sphere_center[2]));
/*
        double dist = Math.abs(prom_dn.distance.x*prom_dn.distance.x + prom_dn.distance.y*prom_dn.distance.y +prom_dn.distance.z*prom_dn.distance.z);
        prom_dn.normal.x = prom_dn.distance.x / dist;
        prom_dn.normal.y = prom_dn.distance.y / dist;
        prom_dn.normal.z = prom_dn.distance.z / dist;
*/

        System.out.printf("4444.n_xz:%7.4f alfa:%7.4f beta:%7.4f %n", n_xz, Math.toDegrees(alfa), Math.toDegrees(beta));
        System.out.printf("5555.prom_dn_dist_dx:%7.4f dy:%7.4f dz:%7.4f prom_dn_normal_nx:%7.4f ny:%7.4f nz:%7.4f%n",
                prom_dn.distance.x, prom_dn.distance.y, prom_dn.distance.z, prom_dn.normal.x, prom_dn.normal.y, prom_dn.normal.z);
        System.out.println("dan_tosphere_inner*");

        return prom_dn;

    }


    /***********************************************/
    Dist_Norm dan_to_arena_quarter(Dist_Norm dn, Rules rules) {
        System.out.println("dan_to_arena_quarter");
        Dist_Norm dan = new Dist_Norm();
        dan.distance = new Pos();
        dan.normal = new Pos();
        dan.pos = new Pos();

        // встреча с землей
        System.out.printf("11._____dn_pos_x:%5.2f y:%5.2f z:%5.2f _____dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f _____dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f%n",
                dn.pos.x, dn.pos.y, dn.pos.z, dn.distance.x, dn.distance.y, dn.distance.z, dn.normal.x, dn.normal.y, dn.normal.z);

        Dist_Norm prom_dn = dan_to_plane(dn, new double[]{0, 0, 0}, new double[]{0, 1, 0});


        System.out.printf("22.prom_dn_pos_x:%5.2f y:%5.2f z:%5.2f prom_dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f prom_dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f%n",
                prom_dn.pos.x, prom_dn.pos.y, prom_dn.pos.z, prom_dn.distance.x, prom_dn.distance.y, prom_dn.distance.z, prom_dn.normal.x, prom_dn.normal.y, prom_dn.normal.z);

        // встреча с потолком

        prom_dn = minimum(prom_dn, dan_to_plane(prom_dn, new double[]{0, rules.arena.height, 0}, new double[]{0, -1, 0}));


//System.out.println(String.format("33.prom_dn_pos_x:%5.2f y:%5.2f z:%5.2f prom_dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f prom_dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f",
//        prom_dn.pos.x, prom_dn.pos.y, prom_dn.pos.z, prom_dn.distance.x, prom_dn.distance.y, prom_dn.distance.z, prom_dn.normal.x, prom_dn.normal.y, prom_dn.normal.z));
        // сторона x

        prom_dn = minimum(prom_dn, dan_to_plane(prom_dn, new double[]{rules.arena.width / 2, 0, 0}, new double[]{-1, 0, 0}));
//System.out.println(String.format("44.prom_dn_pos_x:%5.2f y:%5.2f z:%5.2f prom_dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f prom_dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f",
//        prom_dn.pos.x, prom_dn.pos.y, prom_dn.pos.z, prom_dn.distance.x, prom_dn.distance.y, prom_dn.distance.z, prom_dn.normal.x, prom_dn.normal.y, prom_dn.normal.z));

        // сторона z (гол)
        prom_dn = minimum(prom_dn, dan_to_plane(prom_dn, new double[]{0, 0, rules.arena.depth / 2 + rules.arena.goal_depth}, new double[]{0, 0, -1}));
        System.out.printf("55.prom_dn_pos_x:%5.2f y:%5.2f z:%5.2f prom_dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f prom_dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f%n",
                prom_dn.pos.x, prom_dn.pos.y, prom_dn.pos.z, prom_dn.distance.x, prom_dn.distance.y, prom_dn.distance.z, prom_dn.normal.x, prom_dn.normal.y, prom_dn.normal.z);
        // сторона z
        double vx = dn.pos.x - (rules.arena.goal_width / 2) - rules.arena.goal_top_radius;
        double vy = dn.pos.y - (rules.arena.goal_height) - rules.arena.goal_top_radius;
        if ((dn.pos.x >= (rules.arena.goal_width / 2) + rules.arena.goal_side_radius) || (dn.pos.y >= rules.arena.goal_height + rules.arena.goal_side_radius)
                || ((vx > 0) && (vy > 0) && (Math.sqrt(vx * vx + vy * vy) >= rules.arena.goal_top_radius + rules.arena.goal_side_radius))) {
            prom_dn = minimum(prom_dn, dan_to_plane(prom_dn, new double[]{0, 0, rules.arena.depth / 2}, new double[]{0, 0, -1}));
//            System.out.println(String.format("66.prom_dn_pos_x:%5.2f y:%5.2f z:%5.2f prom_dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f prom_dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f",
//                    prom_dn.pos.x, prom_dn.pos.y, prom_dn.pos.z, prom_dn.distance.x, prom_dn.distance.y, prom_dn.distance.z, prom_dn.normal.x, prom_dn.normal.y, prom_dn.normal.z));
        }

        // сторона x и гол
        if (dn.pos.z >= (rules.arena.depth / 2) + rules.arena.goal_side_radius) {
            // x
            prom_dn = minimum(prom_dn, dan_to_plane(prom_dn, new double[]{rules.arena.goal_width / 2, 0, 0}, new double[]{-1, 0, 0}));
            // y
            prom_dn = minimum(prom_dn, dan_to_plane(prom_dn, new double[]{0, rules.arena.goal_height, 0}, new double[]{0, -1, 0}));
        }
        System.out.printf("66.prom_dn_pos_x:%5.2f y:%5.2f z:%5.2f prom_dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f prom_dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f%n",
                prom_dn.pos.x, prom_dn.pos.y, prom_dn.pos.z, prom_dn.distance.x, prom_dn.distance.y, prom_dn.distance.z, prom_dn.normal.x, prom_dn.normal.y, prom_dn.normal.z);


        // Goal back corners

        if (dn.pos.z > (rules.arena.depth / 2 + rules.arena.depth - rules.arena.bottom_radius)) {
//            prom_dn = minimum(prom_dn, dan_to_sphere_inner(prom_dn, new double[]{0,  rules.arena.goal_height,0}, new double[]{0, -1, 0}));
        }

        // нижние углы Bottom corners
        if (dn.pos.y < rules.arena.bottom_radius) {
            // сторона x
            if (dn.pos.x > rules.arena.width / 2 - rules.arena.bottom_radius) {
//                Dist_Norm dan_to_sphere_inner(Dist_Norm dn, Rules rules , double[] sphere_center, double sphere_radius, double ARENA_E) {
                prom_dn = minimum(prom_dn, dan_to_sphere_inner(prom_dn, new double[]{rules.arena.width / 2 - rules.arena.bottom_radius, rules.arena.bottom_radius, prom_dn.pos.z}, rules.arena.bottom_radius, rules.BALL_ARENA_E));
                System.out.printf("77.prom_dn_pos_x:%5.2f y:%5.2f z:%5.2f prom_dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f prom_dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f%n",
                        prom_dn.pos.x, prom_dn.pos.y, prom_dn.pos.z, prom_dn.distance.x, prom_dn.distance.y, prom_dn.distance.z, prom_dn.normal.x, prom_dn.normal.y, prom_dn.normal.z);


            }
            // сторона z Side z
            if (dn.pos.z > rules.arena.depth / 2 - rules.arena.bottom_radius) {
                prom_dn = minimum(prom_dn, dan_to_sphere_inner(prom_dn, new double[]{prom_dn.pos.x, rules.arena.bottom_radius, rules.arena.depth / 2 - rules.arena.bottom_radius}, rules.arena.bottom_radius, rules.BALL_ARENA_E));
                System.out.printf("88.prom_dn_pos_x:%5.2f y:%5.2f z:%5.2f prom_dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f prom_dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f%n",
                        prom_dn.pos.x, prom_dn.pos.y, prom_dn.pos.z, prom_dn.distance.x, prom_dn.distance.y, prom_dn.distance.z, prom_dn.normal.x, prom_dn.normal.y, prom_dn.normal.z);

            }

            // сторона z Side z (goal)
            if (dn.pos.z > rules.arena.depth / 2 + rules.arena.goal_depth - rules.arena.bottom_radius) {
                prom_dn = minimum(prom_dn, dan_to_sphere_inner(prom_dn, new double[]{prom_dn.pos.x, rules.arena.bottom_radius, rules.arena.depth / 2 + rules.arena.goal_depth - rules.arena.bottom_radius}, rules.arena.bottom_radius, rules.BALL_ARENA_E));
                System.out.printf("99.prom_dn_pos_x:%5.2f y:%5.2f z:%5.2f prom_dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f prom_dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f%n",
                        prom_dn.pos.x, prom_dn.pos.y, prom_dn.pos.z, prom_dn.distance.x, prom_dn.distance.y, prom_dn.distance.z, prom_dn.normal.x, prom_dn.normal.y, prom_dn.normal.z);

            }


        }

        // Ceiling corners
        if (dn.pos.y > rules.arena.height - rules.arena.bottom_radius) {
            // сторона x
            if (dn.pos.x > rules.arena.width / 2 - rules.arena.top_radius) {
//                Dist_Norm dan_to_sphere_inner(Dist_Norm dn, Rules rules , double[] sphere_center, double sphere_radius, double ARENA_E) {
                prom_dn = minimum(prom_dn, dan_to_sphere_inner(prom_dn, new double[]{rules.arena.width / 2 - rules.arena.top_radius, rules.arena.height - rules.arena.top_radius, prom_dn.pos.z}, rules.arena.top_radius, rules.BALL_ARENA_E));
                System.out.printf("100.prom_dn_pos_x:%5.2f y:%5.2f z:%5.2f prom_dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f prom_dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f%n",
                        prom_dn.pos.x, prom_dn.pos.y, prom_dn.pos.z, prom_dn.distance.x, prom_dn.distance.y, prom_dn.distance.z, prom_dn.normal.x, prom_dn.normal.y, prom_dn.normal.z);

            }
            // Side z
            if (dn.pos.z > rules.arena.depth / 2 - rules.arena.top_radius) {
                prom_dn = minimum(prom_dn, dan_to_sphere_inner(prom_dn, new double[]{prom_dn.pos.x, rules.arena.height - rules.arena.top_radius, rules.arena.depth / 2 - rules.arena.top_radius}, rules.arena.top_radius, rules.BALL_ARENA_E));
                System.out.printf("110.prom_dn_pos_x:%5.2f y:%5.2f z:%5.2f prom_dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f prom_dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f%n",
                        prom_dn.pos.x, prom_dn.pos.y, prom_dn.pos.z, prom_dn.distance.x, prom_dn.distance.y, prom_dn.distance.z, prom_dn.normal.x, prom_dn.normal.y, prom_dn.normal.z);

            }
            // Corner
            if ((dn.pos.x > rules.arena.width / 2 - rules.arena.corner_radius) && (dn.pos.z > rules.arena.depth / 2 - rules.arena.corner_radius)) {
                double corner_o_x = rules.arena.width / 2 - rules.arena.corner_radius;
                double corner_o_z = rules.arena.depth / 2 - rules.arena.corner_radius;
                double dv_x = dn.pos.x - corner_o_x;
                double dv_z = dn.pos.z - corner_o_z;
                double dv = Math.sqrt(dv_x * dv_x + dv_z * dv_z);
                if (dv > rules.arena.corner_radius - rules.arena.top_radius) {
                    double n_x = dv_x * 1 / dv;
                    double n_z = dv_z * 1 / dv;
                    double o2_x = corner_o_x + n_x * (rules.arena.corner_radius - rules.arena.top_radius);
                    double o2_z = corner_o_z + n_z * (rules.arena.corner_radius - rules.arena.top_radius);
                    prom_dn = minimum(prom_dn, dan_to_sphere_inner(prom_dn, new double[]{o2_x, rules.arena.height - rules.arena.top_radius, o2_z}, rules.arena.top_radius, rules.BALL_ARENA_E));
                    System.out.printf("120.prom_dn_pos_x:%5.2f y:%5.2f z:%5.2f prom_dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f prom_dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f%n",
                            prom_dn.pos.x, prom_dn.pos.y, prom_dn.pos.z, prom_dn.distance.x, prom_dn.distance.y, prom_dn.distance.z, prom_dn.normal.x, prom_dn.normal.y, prom_dn.normal.z);

                }

            }


        }


        System.out.printf("77.prom_dn_pos_x:%5.2f y:%5.2f z:%5.2f prom_dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f prom_dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f%n",
                prom_dn.pos.x, prom_dn.pos.y, prom_dn.pos.z, prom_dn.distance.x, prom_dn.distance.y, prom_dn.distance.z, prom_dn.normal.x, prom_dn.normal.y, prom_dn.normal.z);

        return prom_dn;
    }

    /***********************************************/
    Dist_Norm dan_to_arena(Dist_Norm dn, Rules rules) {
        System.out.println("*dan_to_arena");
        boolean negative_x = false;
        if (dn.pos.x < 0) {
            negative_x = true;
            dn.pos.x = -dn.pos.x;
        }
        boolean negative_z = false;
        if (dn.pos.z < 0) {
            negative_z = true;
            dn.pos.z = -dn.pos.z;
        }
        System.out.printf("1.dn_pos_x:%5.2f y:%5.2f z:%5.2f dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f%n",
                dn.pos.x, dn.pos.y, dn.pos.z, dn.distance.x, dn.distance.y, dn.distance.z, dn.normal.x, dn.normal.y, dn.normal.z);

        dn = dan_to_arena_quarter(dn, rules);

        System.out.printf("2.dn_pos_x:%5.2f y:%5.2f z:%5.2f dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f%n",
                dn.pos.x, dn.pos.y, dn.pos.z, dn.distance.x, dn.distance.y, dn.distance.z, dn.normal.x, dn.normal.y, dn.normal.z);

        if (negative_x) {
            dn.pos.x = -dn.pos.x;
            dn.normal.x = -dn.normal.x;
        }
        if (negative_z) {
            dn.pos.z = -dn.pos.z;
            dn.normal.z = -dn.normal.z;
        }

        System.out.printf("3.dn_pos_x:%5.2f y:%5.2f z:%5.2f dn_dist_dx:%5.2f dy:%5.2f dz:%5.2f dn_normal_nx:%5.2f ny:%5.2f nz:%5.2f%n",
                dn.pos.x, dn.pos.y, dn.pos.z, dn.distance.x, dn.distance.y, dn.distance.z, dn.normal.x, dn.normal.y, dn.normal.z);

        System.out.println("dan_to_arena*");
        return dn;
    }

    //**************************************
    Object collide_with_arena(Object e, Rules rules) {
        Object obj = new Object();
        obj.pos = new Pos();
        obj.vel = new Vel();
        Dist_Norm dn = new Dist_Norm();
        dn.distance = new Pos();
        dn.normal = new Pos();
        dn.pos = new Pos();

        dn.pos.x = e.pos.x;
        dn.pos.y = e.pos.y;
        dn.pos.z = e.pos.z;

        System.out.printf("1.collide_with_arena_pos_x:%5.2f y:%5.2f z:%5.2f%n", dn.pos.x, dn.pos.y, dn.pos.z);
        System.out.printf("2.e_pos_x:%5.2f y:%5.2f z:%5.2f radius:%5.2f e_vel_x:%5.2f y:%5.2f z:%5.2f%n", e.pos.x, e.pos.y, e.pos.z, e.radius, e.vel.vx, e.vel.vy, e.vel.vz);
        dn = dan_to_arena(dn, rules);
        System.out.printf("3.collide_with_arena_pos_x:%5.2f y:%5.2f z:%5.2f radius:%5.2f%n", dn.pos.x, dn.pos.y, dn.pos.z, e.radius);

        e.pos.x = dn.pos.x;
        e.pos.y = dn.pos.y;
        e.pos.z = dn.pos.z;
        System.out.printf("4.e_pos_x:%5.2f y:%5.2f z:%5.2f radius:%5.2f e_dist_dx:%5.2f dy:%5.2f dz:%5.2f e_normal_nx:%5.2f ny:%5.2f nz:%5.2f%n",
                e.pos.x, e.pos.y, e.pos.z, e.radius, dn.distance.x, dn.distance.y, dn.distance.z, dn.normal.x, dn.normal.y, dn.normal.z);
        Pos penetration = new Pos();

        penetration.x = e.radius - dn.distance.x;
        penetration.y = e.radius - dn.distance.y;
        penetration.z = e.radius - dn.distance.z;
        System.out.printf("5.penetration_x:%5.2f y:%5.2f z:%5.2f%n", penetration.x, penetration.y, penetration.z);
        if (penetration.x > 0) {
            e.pos.x = e.pos.x + penetration.x * dn.normal.x;
            double vx = e.vel.vx * dn.normal.x;
            if (vx < 0) {
                e.vel.vx = e.vel.vx - vx * dn.normal.x;
            }
            if (dn.normal.x != 0) {
                collide_with_object = true;
                System.out.println("Контакт c ареной X");
            }
        }
        if (penetration.y > 0) {
            System.out.println("6.");
            e.pos.y = e.pos.y + penetration.y * dn.normal.y;
            double vy = e.vel.vy * dn.normal.y;
            if (vy < 0) {
                e.vel.vy = e.vel.vy - (1 + rules.BALL_ARENA_E) * vy * dn.normal.y;
            }
            if (dn.normal.y != 0) {
                collide_with_object = true;
                System.out.println("Контакт c ареной Y");
            }
        }
        if (penetration.z > 0) {
            e.pos.z = e.pos.z + penetration.z * dn.normal.z;
            double vz = e.vel.vz - e.vel.vz * dn.normal.z;
            if (vz < 0) {
                e.vel.vz = e.vel.vz - dn.normal.z;
            }
            if (dn.normal.z != 0) {
                collide_with_object = true;
                System.out.println("Контакт c ареной Z");
            }

        }

        System.out.printf("7.e_pos_x:%5.2f y:%5.2f z:%5.2f radius:%5.2f e_vel_x:%5.2f y:%5.2f z:%5.2f%n", e.pos.x, e.pos.y, e.pos.z, e.radius, e.vel.vx, e.vel.vy, e.vel.vz);

        return e;
    }

    void predictBall(Robot me, Rules rules, Game game, Action action) { // предсказание траектории мяча
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format("\n**predictBall");
        }
        System.out.println("predictball_tick:" + game.current_tick);
        ball_arr[0].pos.x = game.ball.x;
        ball_arr[0].pos.y = game.ball.y;
        ball_arr[0].pos.z = game.ball.z;
        ball_arr[0].vel.vx = game.ball.velocity_x;
        ball_arr[0].vel.vy = game.ball.velocity_y;
        ball_arr[0].vel.vz = game.ball.velocity_z;
        ball_arr[0].radius = game.ball.radius;
//      System.out.print("33");


        for (int i = 1; i < PREDICT_POINTS - 1; i++) {
            ball_arr[i].pos.x = ball_arr[i - 1].pos.x + ball_arr[i - 1].vel.vx * 1 / rules.TICKS_PER_SECOND;
            ball_arr[i].pos.y = ball_arr[i - 1].pos.y + ball_arr[i - 1].vel.vy * 1 / rules.TICKS_PER_SECOND;
            ball_arr[i].pos.z = ball_arr[i - 1].pos.z + ball_arr[i - 1].vel.vz * 1 / rules.TICKS_PER_SECOND;
            ball_arr[i].radius = ball_arr[i - 1].radius;
            ball_arr[i].vel.vx = ball_arr[i - 1].vel.vx;
            ball_arr[i].vel.vy = ball_arr[i - 1].vel.vy - rules.GRAVITY * 1 / rules.TICKS_PER_SECOND;
            ball_arr[i].vel.vz = ball_arr[i - 1].vel.vz;

            Object ballobj = new Object();
            ballobj.pos = new Pos();
            ballobj.vel = new Vel();
            ballobj.pos.x = ball_arr[i].pos.x;
            ballobj.pos.y = ball_arr[i].pos.y;
            ballobj.pos.z = ball_arr[i].pos.z;
            ballobj.vel.vx = ball_arr[i].vel.vx;
            ballobj.vel.vy = ball_arr[i].vel.vy;
            ballobj.vel.vz = ball_arr[i].vel.vz;
            ballobj.radius = ball_arr[i].radius;

            Dist_Norm dn = new Dist_Norm();
            Object obj = new Object();

            System.out.printf("i:%d ballobj_x:%5.2f y:%5.2f z:%5.2f%n", i, ballobj.pos.x, ballobj.pos.y, ballobj.pos.z);

            collide_with_object = false;

            obj = collide_with_arena(ballobj, rules);

            if (collide_with_object == true) { // запуск микротиков
                String s = String.format("*Microtics start* i=%d\n", i);
                System.out.printf("*Microtics start* i=%d\n", i);
                graphsToScr += textToSCR(s);
                ball_arr_micro[0].pos.x = ball_arr[i - 1].pos.x;
                ball_arr_micro[0].pos.y = ball_arr[i - 1].pos.y;
                ball_arr_micro[0].pos.z = ball_arr[i - 1].pos.z;
                ball_arr_micro[0].vel.vx = ball_arr[i - 1].vel.vx;
                ball_arr_micro[0].vel.vy = ball_arr[i - 1].vel.vy;
                ball_arr_micro[0].vel.vz = ball_arr[i - 1].vel.vz;
                ball_arr_micro[0].radius = game.ball.radius;
                for (int j = 1; j < rules.MICROTICKS_PER_TICK; j++) {
                    System.out.printf("*Microtick i=%d j=%d\n", i, j);
                    ball_arr_micro[j].pos.x = ball_arr_micro[j - 1].pos.x + ball_arr_micro[j - 1].vel.vx * 1 / rules.TICKS_PER_SECOND / rules.MICROTICKS_PER_TICK;
                    ball_arr_micro[j].pos.y = ball_arr_micro[j - 1].pos.y + ball_arr_micro[j - 1].vel.vy * 1 / rules.TICKS_PER_SECOND / rules.MICROTICKS_PER_TICK;
                    ball_arr_micro[j].pos.z = ball_arr_micro[j - 1].pos.z + ball_arr_micro[j - 1].vel.vz * 1 / rules.TICKS_PER_SECOND / rules.MICROTICKS_PER_TICK;
                    ball_arr_micro[j].radius = ball_arr_micro[j - 1].radius;
                    ball_arr_micro[j].vel.vx = ball_arr_micro[j - 1].vel.vx;
                    ball_arr_micro[j].vel.vy = ball_arr_micro[j - 1].vel.vy - rules.GRAVITY * 1 / rules.TICKS_PER_SECOND / rules.MICROTICKS_PER_TICK;
                    ball_arr_micro[j].vel.vz = ball_arr_micro[j - 1].vel.vz;


                    ballobj.pos.x = ball_arr_micro[j].pos.x;
                    ballobj.pos.y = ball_arr_micro[j].pos.y;
                    ballobj.pos.z = ball_arr_micro[j].pos.z;
                    ballobj.vel.vx = ball_arr_micro[j].vel.vx;
                    ballobj.vel.vy = ball_arr_micro[j].vel.vy;
                    ballobj.vel.vz = ball_arr_micro[j].vel.vz;
                    ballobj.radius = ball_arr_micro[j].radius;

                    obj = collide_with_arena(ballobj, rules);
                    ball_arr_micro[j].pos.x = obj.pos.x;
                    ball_arr_micro[j].pos.y = obj.pos.y;
                    ball_arr_micro[j].pos.z = obj.pos.z;
                    ball_arr_micro[j].vel.vx = obj.vel.vx;
                    ball_arr_micro[j].vel.vy = obj.vel.vy;
                    ball_arr_micro[j].vel.vz = obj.vel.vz;
                    ball_arr_micro[j].radius = obj.radius;
                }

            }


            System.out.printf("*i:%d ballobj_x:%5.2f y:%5.2f z:%5.2f%n", i, ballobj.pos.x, ballobj.pos.y, ballobj.pos.z);

            ball_arr[i].pos.x = obj.pos.x;
            ball_arr[i].pos.y = obj.pos.y;
            ball_arr[i].pos.z = obj.pos.z;
            ball_arr[i].vel.vx = obj.vel.vx;
            ball_arr[i].vel.vy = obj.vel.vy;
            ball_arr[i].vel.vz = obj.vel.vz;
            ball_arr[i].radius = obj.radius;



/*
            if (ball_arr[i].pos.y < game.ball.radius){ // если мяч упал, то отскок
                ball_arr[i].pos.y = game.ball.radius;
                ball_arr[i].vel.vy = -rules.BALL_ARENA_E*ball_arr[i].vel.vy;
            }

            if (ball_arr[i].pos.y > rules.arena.height-ball_arr[i].radius){ // если мяч стукнулся о потолок
                ball_arr[i].pos.y = rules.arena.height-ball_arr[i].radius;
                ball_arr[i].vel.vy = -rules.BALL_ARENA_E*ball_arr[i].vel.vy;
            }

            if (ball_arr[i].pos.x > rules.arena.width/2 - game.ball.radius) {
                ball_arr[i].pos.x = rules.arena.width/2 - game.ball.radius;
                ball_arr[i].vel.vx = - rules.BALL_ARENA_E*ball_arr[i].vel.vx;
            }


            if (ball_arr[i].pos.x < -rules.arena.width/2 + game.ball.radius) {
                ball_arr[i].pos.x = -rules.arena.width/2 + game.ball.radius;
                ball_arr[i].vel.vx = - rules.BALL_ARENA_E*ball_arr[i].vel.vx;
            }
            if (ball_arr[i].pos.z > rules.arena.depth/2 - game.ball.radius) {
                ball_arr[i].pos.z = rules.arena.depth/2 - game.ball.radius;
                ball_arr[i].vel.vz = - rules.BALL_ARENA_E*ball_arr[i].vel.vz;
            }
            if (ball_arr[i].pos.z < -rules.arena.depth/2 + game.ball.radius) {
                ball_arr[i].pos.z = -rules.arena.depth/2 + game.ball.radius;
                ball_arr[i].vel.vz = - rules.BALL_ARENA_E*ball_arr[i].vel.vz;
            }
*/
        }

        if (DEB_ON_SCR) {
            for (int i = 0; i < 20; i++) {
                strToScr += String.format("*bl_arr:%3d:(%3d)", i, i + game.current_tick);
                strToScr += String.format(" bl_x:%5.2f y:%5.2f z:%5.2f", ball_arr[i].pos.x, ball_arr[i].pos.y, ball_arr[i].pos.z);
                strToScr += String.format(" bl_vx:%5.2f vy:%5.2f vz:%5.2f vs:%5.2f", ball_arr[i].vel.vx, ball_arr[i].vel.vy, ball_arr[i].vel.vz,
                        Math.sqrt(ball_arr[i].vel.vx * ball_arr[i].vel.vx + ball_arr[i].vel.vy * ball_arr[i].vel.vy + ball_arr[i].vel.vz * ball_arr[i].vel.vz));
                if (i % 4 == 0) {
                    strToScr += "*\n";
                }
            }
        }
        if (GRAPH_ON_SCR) {
            for (int i = 1; i < PREDICT_POINTS; i++) {
                ScrColor scrcolor = new ScrColor();
                scrcolor.r = 0;
                scrcolor.g = 1;
                scrcolor.b = 0;
                scrcolor.a = 0.5;

                graphsToScr += lineToSCR(ball_arr[i].pos, ball_arr[i - 1].pos, scrcolor, 2);
                graphsToScr += sphereToSCR(ball_arr[i - 1].pos, 0.5, scrcolor);
            }
        }
        if (DEB_ON_SCR) {
            strToScr += "predictBall**\n";
        }
    }


    /***********************************************/
    double delta(double a, double b) {
        return b - a;
    }

    /***********************************************/
    int tryToPoint(Robot me, Rules rules, Game game, Action action) { // расчет кол-ва точек до встречи c мячом
        if (DEBUG_ON) {
            System.out.print("333");
        }

//      strToScr = strToScr+String.format("**me_x:%5.2f: ",me.x);
        rob_arr[0].pos.x = me.x;
        rob_arr[0].pos.y = me.y;
        rob_arr[0].pos.z = me.z;
        rob_arr[0].vel.vx = me.velocity_x;
        rob_arr[0].vel.vy = me.velocity_y;
        rob_arr[0].vel.vz = me.velocity_z;
        rob_arr[0].acc.ax = 0;
        rob_arr[0].acc.ax = 0;
        rob_arr[0].touch = me.touch;
        rob_arr[0].radius = me.radius;
        if (DEBUG_ON) {
            System.out.printf("#dx=%5.2f #dz=%5.2f", delta(ball_arr[0].pos.x, rob_arr[0].pos.x), delta(ball_arr[0].pos.z, rob_arr[0].pos.z));
        }
        rob_arr[0].alfa = Math.atan(delta(ball_arr[0].pos.z, rob_arr[0].pos.z) / delta(ball_arr[0].pos.x, rob_arr[0].pos.x));

        int CurTick = 1; // расчетный шаг мяча
        boolean istouch = false;
        while ((!istouch) && (CurTick < PREDICT_POINTS)) {
            int i = 1;
            //System.out.println("i:"+i);
            while ((!istouch) && (i <= CurTick)) {
//              System.out.println("CurTick:"+CurTick);

                double Alfa = -Math.toRadians(90);
                if (delta(ball_arr[CurTick].pos.x, rob_arr[i - 1].pos.x) != 0) {
                    Alfa = Math.atan(delta(ball_arr[CurTick].pos.z, rob_arr[i - 1].pos.z) / delta(ball_arr[CurTick].pos.x, rob_arr[i - 1].pos.x));
                }
                rob_arr[i].alfa = Alfa;
                if (rob_arr[i - 1].touch) {
                    rob_arr[i].acc.ax = 0 + rules.ROBOT_ACCELERATION * Math.cos(Alfa);
                    rob_arr[i].acc.az = 0 + rules.ROBOT_ACCELERATION * Math.sin(Alfa);

                    rob_arr[i].vel.vx = rob_arr[i - 1].vel.vx - rob_arr[i].acc.ax * 1 / rules.TICKS_PER_SECOND;
                    rob_arr[i].vel.vy = 0;//rob_arr[i].vel.vy - rules.GRAVITY * 1 / rules.TICKS_PER_SECOND;
                    rob_arr[i].vel.vz = rob_arr[i - 1].vel.vz - rob_arr[i].acc.az * 1 / rules.TICKS_PER_SECOND;

                    double vel = Math.sqrt(rob_arr[i].vel.vx * rob_arr[i].vel.vx + rob_arr[i].vel.vz * rob_arr[i].vel.vz);
                    if (vel > rules.ROBOT_MAX_GROUND_SPEED) {
                        rob_arr[i].vel.vx = rob_arr[i].vel.vx * (rules.ROBOT_MAX_GROUND_SPEED / vel);
                        rob_arr[i].vel.vz = rob_arr[i].vel.vz * (rules.ROBOT_MAX_GROUND_SPEED / vel);
                    }
                    rob_arr[i].touch = true;
                    rob_arr[i].radius = rob_arr[i - 1].radius;
                } else {
                    rob_arr[i].acc.ax = 0;
                    rob_arr[i].acc.az = 0;
                    rob_arr[i].vel.vx = rob_arr[i - 1].vel.vx;
                    rob_arr[i].vel.vy = rob_arr[i - 1].vel.vy - rules.GRAVITY * 1 / rules.TICKS_PER_SECOND;
                    rob_arr[i].vel.vz = rob_arr[i - 1].vel.vz;
                    rob_arr[i].touch = false;
                    rob_arr[i].radius = rob_arr[i - 1].radius;

                }
                rob_arr[i].pos.x = rob_arr[i - 1].pos.x + rob_arr[i - 1].vel.vx * 1 / rules.TICKS_PER_SECOND;
                rob_arr[i].pos.y = rob_arr[i - 1].pos.y + rob_arr[i - 1].vel.vy * 1 / rules.TICKS_PER_SECOND;
                rob_arr[i].pos.z = rob_arr[i - 1].pos.z + rob_arr[i - 1].vel.vz * 1 / rules.TICKS_PER_SECOND;


                if (rob_arr[i].pos.y < me.radius) { // если робот упал,
                    rob_arr[i].pos.y = me.radius;
                    rob_arr[i].vel.vy = 0;
                    rob_arr[i].touch = true;
                }
                if (rob_arr[i].pos.x > rules.arena.depth / 2 - me.radius) {
                    rob_arr[i].pos.x = rules.arena.depth / 2 - me.radius;
                    rob_arr[i].vel.vx = -rob_arr[i].vel.vx;
                }
                if (rob_arr[i].pos.x < -rules.arena.depth / 2 + me.radius) {
                    rob_arr[i].pos.x = -rules.arena.depth / 2 + me.radius;
                    rob_arr[i].vel.vx = -rob_arr[i].vel.vx;
                }
                if (rob_arr[i].pos.z > rules.arena.width / 2 - me.radius) {
                    rob_arr[i].pos.z = rules.arena.width / 2 - me.radius;
                    rob_arr[i].vel.vz = -rob_arr[i].vel.vz;
                }
                if (rob_arr[i].pos.z < -rules.arena.width / 2 + me.radius) {
                    rob_arr[i].pos.z = -rules.arena.width / 2 + me.radius;
                    rob_arr[i].vel.vz = -rob_arr[i].vel.vz;
                }

                if ((Math.abs(delta(ball_arr[CurTick].pos.x, rob_arr[i].pos.x)) < (rob_arr[i].radius / 2 + ball_arr[i].radius / 2) &&
                        (Math.abs(delta(ball_arr[CurTick].pos.z, rob_arr[i].pos.z)) < (rob_arr[i].radius / 2 + ball_arr[i].radius / 2)))) {
                    istouch = true;
                }

                if (DEB_ON_SCR && i == 20 && CurTick == 20 /*istouch*/) {

                    strToScr = strToScr + String.format("Alfa:%6.2f(%6.2f)", Alfa, Math.toDegrees(Alfa));
/*
                  for (int j=0; (j<=i); j++){
                      strToScr = strToScr+String.format("*rb_ar:%3d:(%3d): Alfa:%6.2f",j,j+game.current_tick-1,Math.toDegrees(rob_arr[j].alfa));
                      strToScr = strToScr+String.format(" rb:%6.2f :%6.2f :%6.2f",rob_arr[j].pos.x,rob_arr[j].pos.y,rob_arr[j].pos.z);
                      strToScr = strToScr+String.format(" rb_vel:%6.2f :%6.2f :%6.2f",rob_arr[j].vel.vx,rob_arr[j].vel.vy,rob_arr[j].vel.vz);
                      strToScr = strToScr+String.format(" rb_acc:%6.2f :%6.2f :%6.2f",rob_arr[j].acc.ax,rob_arr[j].acc.ay,rob_arr[j].acc.az);
                      strToScr = strToScr+String.format(" rb_vl_sum:%6.2f rb_ac_sum:%6.2f", Math.sqrt(rob_arr[j].vel.vx*rob_arr[j].vel.vx+rob_arr[j].vel.vz*rob_arr[j].vel.vz),
                              Math.sqrt(rob_arr[j].acc.ax*rob_arr[j].acc.ax+rob_arr[j].acc.az*rob_arr[j].acc.az));
                      strToScr = strToScr+String.format(" rb_tch:%b",istouch);
                      strToScr = strToScr+String.format("*****\n");
                  }
                  strToScr = strToScr+String.format("**\n");
*/
                }
                i++;
            }

            CurTick++;


//          System.out.println("Cur/tick:"+CurTick+" i:"+i);
        }
        //    System.out.println("555");

//      strToScr = strToScr+String.format("**touch:%b:tick:%d\n",istouch,CurTick);

        if (istouch) {
            return CurTick - 1;
        } else
            return PREDICT_POINTS + 1;
    }

    /***********************************************/
    Robot2 tryToPoint2(Robot me, Rules rules, Game game, Action action) { // расчет кол-ва точек до встречи c мячом
        if (DEB_ON_SCR) {
            strToScr += "\n**tryToPoint2";
        }
        if (DEBUG_ON) {
            System.out.print("\n***tryToPoint2");
        }
        Robot2 next_step = new Robot2(); // структура хранения результата
        next_step.rob = new Robot();
        next_step.act = new Action();
        if (DEBUG_ON) {
            System.out.print(" 333");
        }

//      strToScr = strToScr+String.format("**me_x:%5.2f: ",me.x);
        rob_arr[0].pos.x = me.x;
        rob_arr[0].pos.y = me.y;
        rob_arr[0].pos.z = me.z;
        rob_arr[0].vel.vx = me.velocity_x;
        rob_arr[0].vel.vy = me.velocity_y;
        rob_arr[0].vel.vz = me.velocity_z;
        rob_arr[0].acc.ax = 0;
        rob_arr[0].acc.az = 0;
        rob_arr[0].touch = me.touch;
        rob_arr[0].radius = me.radius;

        double dz = delta(ball_arr[0].pos.z, rob_arr[0].pos.z);
        double dx = delta(ball_arr[0].pos.x, rob_arr[0].pos.x);
        if (DEBUG_ON) {
            System.out.printf(" #dx:%5.2f dz:%5.2f", dx, dz);
        }
        rob_arr[0].alfa = Math.atan(90);
        if (dx != 0) {
            rob_arr[0].alfa = Math.atan(dz / dx);
        }
//  System.out.println("#ra_a:"+rob_arr[0].alfa);
        if (DEBUG_ON) {
            System.out.printf(" #alfa:%5.2f", Math.toDegrees(rob_arr[0].alfa));
        }

        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format(" rb0_x:%5.2f y:%5.2f z:%5.2f alfa:%5.2f \n", rob_arr[0].pos.x, rob_arr[0].pos.y, rob_arr[0].pos.z, Math.toDegrees(rob_arr[0].alfa));
        }

        int CurTick = 1; // расчетный шаг мяча
        boolean istouch = false;
        if (DEB_ON_SCR) {
            strToScr += "#0=";
        }
        boolean istouch_all = false;
        while ((!istouch) && (CurTick < DEFEND_PREDICT_POINTS)) {
            if (DEB_ON_SCR) {
                strToScr += String.format("T%d|", CurTick);
            }
            int i = 1;
            //System.out.println("i:"+i);
            if (DEBUG_ON) {
                System.out.printf("\n ball_x:%5.2f ball_z:%5.2f ", ball_arr[CurTick].pos.x, ball_arr[CurTick].pos.z);
            }
            while ((!istouch) && (i <= CurTick)) {
                if (DEBUG_ON) {
                    System.out.print("CurTick:" + CurTick + " i:" + i);
                }
                if (DEB_ON_SCR) {
                    strToScr += String.format("i%d|", i);
                }
//              System.out.println("CurTick:"+CurTick);
                double Alfa = Math.toRadians(90);
                dz = delta(ball_arr[CurTick].pos.z, rob_arr[i - 1].pos.z);
                dx = delta(ball_arr[CurTick].pos.x, rob_arr[i - 1].pos.x);

                if (dx != 0) {
                    Alfa = Math.atan(dz / dx);
                }
                rob_arr[i].alfa = Alfa;
                if (DEBUG_ON) {
                    System.out.printf(" alfa:%5.2f", Math.toDegrees(rob_arr[i].alfa));
                }
                if (rob_arr[i - 1].touch) {
                    if (rob_arr[i].alfa > 0) {
                        rob_arr[i].acc.ax = 0 + rules.ROBOT_ACCELERATION * Math.cos(Alfa);
                        rob_arr[i].acc.az = 0 + rules.ROBOT_ACCELERATION * Math.sin(Alfa);
                    } else {
                        rob_arr[i].acc.ax = 0 - rules.ROBOT_ACCELERATION * Math.cos(Alfa);
                        rob_arr[i].acc.az = 0 - rules.ROBOT_ACCELERATION * Math.sin(Alfa);
                    }
                    if (DEBUG_ON) {
                        System.out.printf(" ax:%5.2f az:%5.2f", rob_arr[i].acc.ax, rob_arr[i].acc.az);
                    }


                    rob_arr[i].vel.vx = rob_arr[i - 1].vel.vx + rob_arr[i].acc.ax * 1 / rules.TICKS_PER_SECOND;
                    rob_arr[i].vel.vy = 0;//rob_arr[i].vel.vy - rules.GRAVITY * 1 / rules.TICKS_PER_SECOND;
                    rob_arr[i].vel.vz = rob_arr[i - 1].vel.vz + rob_arr[i].acc.az * 1 / rules.TICKS_PER_SECOND;
                    if (DEBUG_ON) {
                        System.out.printf(" vx:%5.2f vz:%5.2f", rob_arr[i].vel.vx, rob_arr[i].vel.vz);
                    }

                    double vx = rob_arr[i].vel.vx;
                    double vz = rob_arr[i].vel.vz;
                    double vel = Math.sqrt(vx * vx + vz * vz);
                    if (DEBUG_ON) {
                        System.out.print(" #A");
                    }
                    if (vel > rules.ROBOT_MAX_GROUND_SPEED) {
                        if (DEBUG_ON) {
                            System.out.print(" #B");
                        }
                        rob_arr[i].vel.vx = vx * (rules.ROBOT_MAX_GROUND_SPEED / vel);
                        rob_arr[i].vel.vz = vz * (rules.ROBOT_MAX_GROUND_SPEED / vel);
                        if (DEBUG_ON) {
                            System.out.printf(" vcor_x:%5.2f vcor_z:%5.2f ", rob_arr[i].vel.vx, rob_arr[i].vel.vz);
                        }
                    }
                    rob_arr[i].touch = rob_arr[i - 1].touch;
                    rob_arr[i].radius = rob_arr[i - 1].radius;
                } else {
                    if (DEBUG_ON) {
                        System.out.print(" #C");
                    }
                    rob_arr[i].acc.ax = 0;
                    rob_arr[i].acc.az = 0;
                    rob_arr[i].vel.vx = rob_arr[i - 1].vel.vx;
                    rob_arr[i].vel.vy = rob_arr[i - 1].vel.vy - rules.GRAVITY * 1 / rules.TICKS_PER_SECOND;
                    rob_arr[i].vel.vz = rob_arr[i - 1].vel.vz;
                    rob_arr[i].touch = false;
                    rob_arr[i].radius = rob_arr[i - 1].radius;

                }
                rob_arr[i].pos.x = rob_arr[i - 1].pos.x + rob_arr[i - 1].vel.vx * 1 / rules.TICKS_PER_SECOND;
                rob_arr[i].pos.y = rob_arr[i - 1].pos.y + rob_arr[i - 1].vel.vy * 1 / rules.TICKS_PER_SECOND;
                rob_arr[i].pos.z = rob_arr[i - 1].pos.z + rob_arr[i - 1].vel.vz * 1 / rules.TICKS_PER_SECOND;
                if (DEBUG_ON) {
                    System.out.printf(" xi:%5.2f zi:%5.2f", rob_arr[i].pos.x, rob_arr[i].pos.z);
                }


                if (rob_arr[i].pos.y < me.radius) { // если робот упал,
                    if (DEBUG_ON) {
                        System.out.print(" #D");
                    }
                    rob_arr[i].pos.y = me.radius;
                    rob_arr[i].vel.vy = 0;
                    rob_arr[i].touch = true;
                }
                if (rob_arr[i].pos.x > rules.arena.width / 2 - me.radius) {
                    rob_arr[i].pos.x = rules.arena.width / 2 - me.radius;
                    rob_arr[i].vel.vx = -rob_arr[i].vel.vx;
                    if (DEBUG_ON) {
                        System.out.print(" #E");
                    }
                }
                if (rob_arr[i].pos.x < -rules.arena.width / 2 + me.radius) {
                    rob_arr[i].pos.x = -rules.arena.width / 2 + me.radius;
                    rob_arr[i].vel.vx = -rob_arr[i].vel.vx;
                    if (DEBUG_ON) {
                        System.out.print(" #F");
                    }
                }
                if (rob_arr[i].pos.z > rules.arena.depth / 2 - me.radius) {
                    rob_arr[i].pos.z = rules.arena.depth / 2 - me.radius;
                    rob_arr[i].vel.vz = -rob_arr[i].vel.vz;
                    if (DEBUG_ON) {
                        System.out.print(" #G");
                    }
                }
                if (rob_arr[i].pos.z < -rules.arena.depth / 2 + me.radius) {
                    rob_arr[i].pos.z = -rules.arena.depth / 2 + me.radius;
                    rob_arr[i].vel.vz = -rob_arr[i].vel.vz;
                    if (DEBUG_ON) {
                        System.out.print(" #H");
                    }
                }

                dx = delta(ball_arr[CurTick].pos.x, rob_arr[i].pos.x);
                dz = delta(ball_arr[CurTick].pos.z, rob_arr[i].pos.z);

                if ((Math.abs(dx) < (rob_arr[i].radius / 2 + ball_arr[i].radius / 2) &&
                        (Math.abs(dz) < (rob_arr[i].radius / 2 + ball_arr[i].radius / 2)))) {
                    istouch = true;
                    if (DEBUG_ON) {
                        System.out.print(" #I");
                    }
                }

                if (DEBUG_ON) {
                    System.out.println(" #J***");
                }

                if (DEB_ON_SCR && i == 20 && CurTick == 20 /*istouch*/) {

                    strToScr += String.format("Alfa:%5.2f(%6.2f)", Alfa, Math.toDegrees(Alfa));

                    for (int j = 0; ((j <= i) && (j < 1)); j++) {
                        strToScr += String.format("*r_ar:%2d:(%3d): Alfa:%5.2f", j, j + game.current_tick - 1, Math.toDegrees(rob_arr[j].alfa));
                        strToScr += String.format(" r:%5.2f:%5.2f:%5.2f", rob_arr[j].pos.x, rob_arr[j].pos.y, rob_arr[j].pos.z);
                        strToScr += String.format(" r_vel:%5.2f:%5.2f:%5.2f", rob_arr[j].vel.vx, rob_arr[j].vel.vy, rob_arr[j].vel.vz);
                        strToScr += String.format(" r_acc:%5.2f:%5.2f:%5.2f", rob_arr[j].acc.ax, rob_arr[j].acc.ay, rob_arr[j].acc.az);
                        strToScr += String.format(" r_vl_sum:%5.2f r_ac_sum:%5.2f", Math.sqrt(rob_arr[j].vel.vx * rob_arr[j].vel.vx + rob_arr[j].vel.vz * rob_arr[j].vel.vz),
                                Math.sqrt(rob_arr[j].acc.ax * rob_arr[j].acc.ax + rob_arr[j].acc.az * rob_arr[j].acc.az));
                        dx = delta(ball_arr[j].pos.x, rob_arr[j].pos.x);
                        dz = delta(ball_arr[j].pos.z, rob_arr[j].pos.z);
                        strToScr = strToScr + String.format(" dx:%5.2f|dz:%5.2f|dist:%5.2f|rb_istch:%b", dx, dz, Math.sqrt(dx * dx + dz * dz), istouch);

                        if (j % 2 == 0) {
                            strToScr += "**\n";
                        }
                    }
                    strToScr += "**\n";

                }
                i++;
            }
            CurTick++;

//          System.out.println("Cur/tick:"+CurTick+" i:"+i);
        }
        //    System.out.println("555");

//      strToScr = strToScr+String.format("**touch:%b:tick:%d\n",istouch,CurTick);
        next_step.rob.x = rob_arr[1].pos.x;
        next_step.rob.y = rob_arr[1].pos.y;
        next_step.rob.z = rob_arr[1].pos.z;
        next_step.act.target_velocity_x = rob_arr[1].vel.vx;
        next_step.act.target_velocity_y = rob_arr[1].vel.vy;
        next_step.act.target_velocity_z = rob_arr[1].vel.vz;


        if (istouch) {
            if (DEB_ON_SCR) {
                for (int j = 0; ((j < CurTick - 1)/* && (j < 1)*/); j++) {
                    strToScr = strToScr + String.format("*rb_ar:%3d:(%3d): Alfa:%6.2f", j, j + game.current_tick - 1, Math.toDegrees(rob_arr[j].alfa));
                    strToScr = strToScr + String.format(" rb:%6.2f :%6.2f :%6.2f", rob_arr[j].pos.x, rob_arr[j].pos.y, rob_arr[j].pos.z);
                    strToScr = strToScr + String.format(" rb_vel:%6.2f :%6.2f :%6.2f", rob_arr[j].vel.vx, rob_arr[j].vel.vy, rob_arr[j].vel.vz);
                    strToScr = strToScr + String.format(" rb_acc:%6.2f :%6.2f :%6.2f", rob_arr[j].acc.ax, rob_arr[j].acc.ay, rob_arr[j].acc.az);
                    strToScr = strToScr + String.format(" rb_vl_sum:%6.2f rb_ac_sum:%6.2f", Math.sqrt(rob_arr[j].vel.vx * rob_arr[j].vel.vx + rob_arr[j].vel.vz * rob_arr[j].vel.vz),
                            Math.sqrt(rob_arr[j].acc.ax * rob_arr[j].acc.ax + rob_arr[j].acc.az * rob_arr[j].acc.az));
                    strToScr = strToScr + String.format(" rb_istch:%b", istouch);

                    if (j % 2 == 0) {
                        strToScr += "*****\n";
                    }
                }
            }
            if (GRAPH_ON_SCR) {
                for (int j = 0; ((j < CurTick - 1)/* && (j < 1)*/); j++) {
                    ScrColor scrcolor = new ScrColor();
                    scrcolor.r = 0;
                    scrcolor.g = 0;
                    scrcolor.b = 1;
                    scrcolor.a = 0.5;
                    graphsToScr = graphsToScr + lineToSCR(rob_arr[j + 1].pos, rob_arr[j].pos, scrcolor, 2);
                    graphsToScr = graphsToScr + sphereToSCR(rob_arr[j].pos, 0.5, scrcolor);
                }
            }
            istouch_all = false;
            for (int j = 0; (j < CurTick - 1) && (!istouch_all); j++) {
                //Robot me, Rules rules, Game game, Action action
                Robot prom_rob = new Robot();
                prom_rob.x = rob_arr[j].pos.x;
                prom_rob.y = rob_arr[j].pos.y;
                prom_rob.z = rob_arr[j].pos.z;
                prom_rob.velocity_x = rob_arr[j].vel.vx;
                prom_rob.velocity_y = rob_arr[j].vel.vy;
                prom_rob.velocity_z = rob_arr[j].vel.vz;

                double count = timeToJump4(prom_rob, rules, game, action, j);
                if (count > 0) {
                    istouch_all = true;
                    next_step.count = CurTick - 1;
                }

            }

        } else {
            next_step.count = -1;
        }
        if (DEB_ON_SCR) {
            strToScr += String.format("\n istouch=%b|istouch_all=%b|count=%d ", istouch, istouch_all, next_step.count);
            strToScr += "tryToPoint2**";
        }
        if (DEBUG_ON) {
            System.out.printf("\n istouch=%b|istouch_all=%b|count=%d ", istouch, istouch_all, next_step.count);
            System.out.printf("tryToPoint2**");
        }

        return next_step;

    }

    /***********************************************/
    void printInConsole(Robot me, Rules rules, Game game, Action action) {
        if (DEBUG_ON) {
            System.out.println();
            System.out.printf("T:%5d", game.current_tick);
            System.out.printf(" id:%d ", me.id);
            System.out.printf(" id:%b ", me.is_teammate);
            System.out.printf(" me:%6.3f :%6.3f :%6.3f", me.x, me.y, me.z);
            System.out.printf(" me_vel:%6.3f :%6.3f :%6.3f ", me.velocity_x, me.velocity_y, me.velocity_z);
            System.out.printf(" bl:%6.2f :%6.2f :%6.2f", game.ball.x, game.ball.y, game.ball.z);
            System.out.printf(" bl_vel:%6.2f :%6.2f :%6.2f ", game.ball.velocity_x, game.ball.velocity_y, game.ball.velocity_z);
        }
        if (DEB_ON_SCR) {
            if (game.current_tick == 1) {
                strToScr = strToScr + String.format("VERSION:%s \n", VERSION);
            }
            strToScr = strToScr + String.format("T:%5d act_tick=%5d (from_beg_round=%5d)", game.current_tick, act_tick, game.current_tick - ticks_per_round);
            strToScr = strToScr + String.format(" id:%d ", me.id);
            strToScr = strToScr + String.format(" team:%b ", me.is_teammate);
            strToScr = strToScr + String.format(" x:%5.2f y:%5.2f z:%5.2f", me.x, me.y, me.z);
            strToScr = strToScr + String.format(" vx:%5.2f vy:%5.2f :%5.2f ", me.velocity_x, me.velocity_y, me.velocity_z);
            strToScr = strToScr + String.format("   bl_x:%5.2f y:%5.2f z:%5.2f", game.ball.x, game.ball.y, game.ball.z);
            strToScr = strToScr + String.format("   bl_vx:%5.2f vy:%5.2f vz:%5.2f \n", game.ball.velocity_x, game.ball.velocity_y, game.ball.velocity_z);


            for (int i = 0; i < rules.team_size * 2; i++) {
                for (int j = 0; j < rules.team_size * 2; j++) {
                    if (i + 1 == game.robots[j].id) {
                        strToScr = strToScr + String.format("i:%2d id:%2d", i, game.robots[j].id);
                        strToScr = strToScr + String.format(" me_x:%5.2f y:%5.2f z:%5.2f", game.robots[j].x, game.robots[j].y, game.robots[j].z);
                        strToScr = strToScr + String.format(" vx:%5.2f vy:%5.2f vz:%5.2f vs:%5.2f", game.robots[j].velocity_x, game.robots[j].velocity_y, game.robots[j].velocity_z,
                                Math.sqrt(game.robots[j].velocity_x * game.robots[j].velocity_x + game.robots[j].velocity_y * game.robots[j].velocity_y + game.robots[j].velocity_z * game.robots[j].velocity_z));
                        strToScr = strToScr + String.format(" R:%6.2f team:%b touch:%b", game.robots[j].radius, game.robots[j].is_teammate, game.robots[j].touch);
                        strToScr = strToScr + String.format(" tch:%6.2f :%6.2f :%6.2f \n", game.robots[j].touch_normal_x, game.robots[j].touch_normal_y, game.robots[j].touch_normal_z);
                    }
                }

            }
        }
        if (GRAPH_ON_SCR && (counter % team_size == 0)) {
            String str = String.format("*counter:%d\n", counter);
            str = str + String.format("BALL.x:%5.3f y:%5.3f z:%5.3f radius:%5.2f vx:%5.3f vy:%5.3f vz:%5.3f v_full:%5.3f \n", game.ball.x, game.ball.y, game.ball.z, game.ball.radius, game.ball.velocity_x, game.ball.velocity_y, game.ball.velocity_z,
                    Math.sqrt(game.ball.velocity_x * game.ball.velocity_x + game.ball.velocity_z * game.ball.velocity_z),
                    Math.sqrt(game.ball.velocity_x * game.ball.velocity_x + game.ball.velocity_y * game.ball.velocity_y + game.ball.velocity_z * game.ball.velocity_z));
            for (int i = 0; i < rules.team_size * 2; i++) {
                for (int j = 0; j < rules.team_size * 2; j++) {
                    if (i + 1 == game.robots[j].id) {
                        str += String.format("i:%2d id:%2d", i, game.robots[j].id);
                        str += String.format(" me_x:%5.2f y:%5.2f z:%5.2f", game.robots[j].x, game.robots[j].y, game.robots[j].z);
                        str += String.format(" vx:%5.2f vy:%5.2f vz:%5.2f vxz:%5.2f vxyz:%5.2f", game.robots[j].velocity_x, game.robots[j].velocity_y, game.robots[j].velocity_z,
                                Math.sqrt(game.robots[j].velocity_x * game.robots[j].velocity_x + game.robots[j].velocity_z * game.robots[j].velocity_z),
                                Math.sqrt(game.robots[j].velocity_x * game.robots[j].velocity_x + game.robots[j].velocity_y * game.robots[j].velocity_y + game.robots[j].velocity_z * game.robots[j].velocity_z));
                        str += String.format(" R:%6.2f team:%b nitro:%5.2f touch:%b", game.robots[j].radius, game.robots[j].is_teammate, game.robots[j].nitro_amount, game.robots[j].touch);
                        str += String.format(" tch:%6.2f :%6.2f :%6.2f \n", game.robots[j].touch_normal_x, game.robots[j].touch_normal_y, game.robots[j].touch_normal_z);
                    }
                }
            }
            graphsToScr = graphsToScr + textToSCR(str);
        }
    }

    boolean IsGoal(Game game, Arena arena) {  // проверка есть ли гол
        return Math.abs(game.ball.z) > arena.depth / 2 + game.ball.radius;
    }

    /***********************************************/
    double timeToJump(Robot me, Rules rules, Game game, Action action) {
        if (DEB_ON_SCR) {
            strToScr = strToScr + "\n **TimeToJump";
        }
        Object rob = new Object(); // структура хранения результата
        rob.pos = new Pos();
        rob.vel = new Vel();
        rob.acc = new Acc();
        rob.norm = new Normal();
        boolean contact = false; // встреча с мячом
        boolean good_contact = false;
        int i = 0;
        double vely0 = rules.ROBOT_MAX_JUMP_SPEED;
        rob.pos.x = me.x;
        rob.pos.y = me.y;
        rob.pos.z = me.z;

        while ((vely0 >= 0) && (!contact) && (i < 10)) {
            double dx = ball_arr[i].pos.x - rob.pos.x;
            double dy = ball_arr[i].pos.y - rob.pos.y;
            double dz = ball_arr[i].pos.z - rob.pos.z;
            double distant = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (i < 15) {
                if (DEB_ON_SCR) {
                    strToScr += String.format("i:%2d |dx:%5.2f |dy:%5.2f |dz%5.2f", i, dx, dy, dz);
                }
            }
            if (distant < (/*me.radius+*/ball_arr[i].radius)) { // касание мяча
                contact = true;
                if (rob.pos.z < ball_arr[i].pos.z) {
                    if (rob.pos.y < ball_arr[i].pos.y) {
                        good_contact = true;  //фигачим
                    }

                } else {

                }
            }
            if (i < 15) {
                if (DEB_ON_SCR) {
                    strToScr += String.format(" |dist:%5.2f |ct:%b |gc:%b |vely:%6.2f\n", distant, contact, good_contact, vely0);
                }
            }
            if (!contact) {
                rob.pos.x = rob.pos.x + me.velocity_x / rules.TICKS_PER_SECOND;
                rob.pos.y = rob.pos.y + vely0 / rules.TICKS_PER_SECOND;
                rob.pos.z = rob.pos.z + me.velocity_z / rules.TICKS_PER_SECOND;
                vely0 = vely0 - rules.GRAVITY * 1 / rules.TICKS_PER_SECOND;
                i = i + 1;
            }
        }
        if (DEB_ON_SCR) {
            strToScr += String.format(" TimeToJump**\n");
        }
        if (good_contact) {
            return rules.ROBOT_MAX_JUMP_SPEED;
        } else {
            return -1;
        }

    }

    /***********************************************/
    double timeToJump2(Robot me, Rules rules, Game game, Action action) // Прыжок в центр мяча со старта
    {
        if (DEB_ON_SCR) {
            strToScr += "\n**TimeToJump2\n";
        }
        Robot2 robot = new Robot2(); // структура хранения результата
        robot.rob = new Robot();
        robot.act = new Action();
        boolean contact = false; // встреча с мячом
        boolean good_contact = false;
        int i = 0;
        double vely0 = rules.ROBOT_MAX_JUMP_SPEED;
        double rob__pos_y = me.velocity_y;
        robot.rob.x = me.x;
        robot.rob.y = me.y;
        robot.rob.z = me.z;

        while ((vely0 >= 0) && (!contact) && (i < 10)) {
            double dx = delta(ball_arr[i].pos.x, robot.rob.x);
            double dy = delta(ball_arr[i].pos.y, robot.rob.y);
            double dz = delta(ball_arr[i].pos.z, robot.rob.z);
            double distant = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (i < 1) {
                if (DEB_ON_SCR) {
                    strToScr = strToScr + String.format("  i:(%2d) |x:%5.2f |y:%5.2f |z%5.2f", i, robot.rob.x, robot.rob.y, robot.rob.z);
                    strToScr = strToScr + String.format(" |dx:%5.2f |dy:%5.2f |dz%5.2f", dx, dy, dz);
                }
            }
            if (distant < (me.radius + ball_arr[i].radius)) { // касание мяча
                contact = true;
                if (robot.rob.z < ball_arr[i].pos.z) {
                    if (robot.rob.y < ball_arr[i].pos.y) {
                        good_contact = true;  //фигачим
                    }

                } else {

                }
            }
            if (i < 1) {
                if (DEB_ON_SCR) {
                    strToScr += String.format(" |dist:%5.2f |ct:%b |gc:%b |vely:%6.2f\n", distant, contact, good_contact, vely0);
                }
            }
            if (!contact) {
                robot.rob.x = robot.rob.x + me.velocity_x / rules.TICKS_PER_SECOND;
                robot.rob.y = robot.rob.y + vely0 / rules.TICKS_PER_SECOND;
                robot.rob.z = robot.rob.z + me.velocity_z / rules.TICKS_PER_SECOND;
                vely0 = vely0 - rules.GRAVITY * 1 / rules.TICKS_PER_SECOND;
                i = i + 1;
            }
        }
        if (DEB_ON_SCR) {
            strToScr += "TimeToJump2**\n";
        }
        if (good_contact) {
            return rules.ROBOT_MAX_JUMP_SPEED;
        } else {
            return -1;
        }

    }

    /***********************************************/
    double timeToJump3(Robot me, Rules rules, Game game, Action action) // Прыжок в защите
    {
        if (DEB_ON_SCR) {
            strToScr += "\n**TimeToJump3\n";
        }
        Robot2 robot = new Robot2(); // структура хранения результата
        robot.rob = new Robot();
        robot.act = new Action();
        boolean contact = false; // встреча с мячом
        boolean good_contact = false;
        int i = 0;
        double vely0 = rules.ROBOT_MAX_JUMP_SPEED;
        double rob__pos_y = me.velocity_y;
        robot.rob.x = me.x;
        robot.rob.y = me.y;
        robot.rob.z = me.z;

        while ((vely0 >= 0) && (!contact) && (i < PREDICT_POINTS)) {
            double dx = delta(ball_arr[i].pos.x, robot.rob.x);
            double dy = delta(ball_arr[i].pos.y, robot.rob.y);
            double dz = delta(ball_arr[i].pos.z, robot.rob.z);
            double distant = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if ((DEB_ON_SCR) && (i < 30)) {

                strToScr = strToScr + String.format("  i:(%2d) |x:%5.2f |y:%5.2f |z%5.2f", i, robot.rob.x, robot.rob.y, robot.rob.z);
                strToScr = strToScr + String.format(" |dx:%5.2f |dy:%5.2f |dz%5.2f", dx, dy, dz);
            }
            if (distant < (me.radius + ball_arr[i].radius)) { // касание мяча
                if (DEB_ON_SCR) {
                    strToScr = strToScr + "\n#1";
                }
                contact = true;
                if (robot.rob.z < ball_arr[i].pos.z) {
                    if (DEB_ON_SCR) {
                        strToScr = strToScr + "#2";
                    }
                    if (robot.rob.y < ball_arr[i].pos.y) {
                        if (DEB_ON_SCR) {
                            strToScr = strToScr + "#3";
                        }
                        if (dz < ball_arr[i].radius) {
                            if (DEB_ON_SCR) {
                                strToScr = strToScr + "#4";
                            }
                            good_contact = true;  //фигачим
                            if (DEB_ON_SCR) {
                                strToScr = strToScr + String.format(" i=%2d", i);
                            }

                        }
                    }

                } else {

                }
            }
            if ((DEB_ON_SCR) && (i < 30)) {
                strToScr = strToScr + String.format(" |dist:%5.2f |ct:%b |gc:%b |vely:%6.2f", distant, contact, good_contact, vely0);
                if (i % 4 == 0) {
                    strToScr = strToScr + "\n";
                }
            }
            if (!contact) {
                robot.rob.x = robot.rob.x + me.velocity_x / rules.TICKS_PER_SECOND;
                robot.rob.y = robot.rob.y + vely0 / rules.TICKS_PER_SECOND;
                robot.rob.z = robot.rob.z + me.velocity_z / rules.TICKS_PER_SECOND;
                vely0 = vely0 - rules.GRAVITY * 1 / rules.TICKS_PER_SECOND;
                i = i + 1;
            }
        }
        if (DEB_ON_SCR) {
            strToScr = strToScr + "TimeToJump3**\n";
        }
        if (good_contact) {
            return rules.ROBOT_MAX_JUMP_SPEED;
        } else {
            return -1;
        }

    }

    /***********************************************/
    double timeToJump4(Robot me, Rules rules, Game game, Action action, int offset) // Прыжок в центр мяча со старта со смещением такта
    {
        if (DEB_ON_SCR) {
            strToScr = strToScr + "\n**TimeToJump4\n";
        }
        Robot2 robot = new Robot2(); // структура хранения результата
        Robot2 prev_point = new Robot2(); // структура хранения результата
        robot.rob = new Robot();
        robot.act = new Action();
        prev_point.rob = new Robot();
        prev_point.act = new Action();
        boolean contact = false; // встреча с мячом
        boolean good_contact = false;
        int i = 0;
        double vely0 = rules.ROBOT_MAX_JUMP_SPEED;
        double rob__pos_y = me.velocity_y;
        robot.rob.x = me.x;
        robot.rob.y = me.y;
        robot.rob.z = me.z;
        prev_point = robot;

        while ((vely0 >= 0) && (!contact) && (i + offset < PREDICT_POINTS)) {
            double dx = delta(ball_arr[i + offset].pos.x, robot.rob.x);
            double dy = delta(ball_arr[i + offset].pos.y, robot.rob.y);
            double dz = delta(ball_arr[i + offset].pos.z, robot.rob.z);
            double distant = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if ((DEB_ON_SCR) && (i < 15)) {
                strToScr += String.format("  i:(%2d) |x:%5.2f |y:%5.2f |z%5.2f", i, robot.rob.x, robot.rob.y, robot.rob.z);
                strToScr += String.format(" |dx:%5.2f |dy:%5.2f |dz%5.2f", dx, dy, dz);
            }
            if (distant < (me.radius + ball_arr[i].radius)) { // касание мяча
                contact = true;
                if (robot.rob.z < ball_arr[i + offset].pos.z) {
                    if (robot.rob.y < ball_arr[i + offset].pos.y) {
                        good_contact = true;  //фигачим
                    }

                } else {

                }
            }

            if (GRAPH_ON_SCR) {
                ScrColor scrcolor = new ScrColor();
                scrcolor.r = 1;
                if (!contact) {
                    scrcolor.g = 0;
                } else {
                    scrcolor.g = 1;
                }
                scrcolor.b = 0;
                scrcolor.a = 0.5;
                Pos pos = new Pos();
                pos.x = robot.rob.x;
                pos.y = robot.rob.y;
                pos.z = robot.rob.z;
//                graphsToScr = graphsToScr + lineToSCR(p.pos, rob_arr[j].pos, scrcolor, 2);
                graphsToScr = graphsToScr + sphereToSCR(pos, 0.5, scrcolor);
            }


            if ((DEB_ON_SCR) && (i < 15)) {
                strToScr = strToScr + String.format(" |dist:%5.2f |ct:%b |gc:%b |vely:%6.2f\n", distant, contact, good_contact, vely0);
            }
            if (!contact) {
                robot.rob.x = robot.rob.x + me.velocity_x / rules.TICKS_PER_SECOND;
                robot.rob.y = robot.rob.y + vely0 / rules.TICKS_PER_SECOND;
                robot.rob.z = robot.rob.z + me.velocity_z / rules.TICKS_PER_SECOND;
                vely0 = vely0 - rules.GRAVITY * 1 / rules.TICKS_PER_SECOND;
                i = i + 1;
            }
        }
        if (DEB_ON_SCR) {
            strToScr += "TimeToJump4**\n";
        }
        if (good_contact) {
            return rules.ROBOT_MAX_JUMP_SPEED;
        } else {
            return -1;
        }

    }

    /***********************************************/
    Pos bestAttackCoords(Robot me, Rules rules, Game game)// Расчет координат точки вектора атаки
    {
        if (DEB_ON_SCR) {
            strToScr = strToScr + "\n**bestAttackCoords|";
            strToScr = strToScr + String.format(" ball_x:%5.2f ball_z:%5.2f me.radius:%5.2f ball.radius:%5.2f", game.ball.x, game.ball.z, me.radius, game.ball.radius);
        }
        if (DEBUG_ON) {
            System.out.print("\n**bestAttackCoords|");
            System.out.printf(" ball_x:%5.2f ball_z:%5.2f me.radius:%5.2f ball.radius:%5.2f", game.ball.x, game.ball.z, me.radius, game.ball.radius);
        }

        Pos pos = new Pos();
        double enemy_goal_line = rules.arena.depth / 2 + game.ball.radius;
        double dz = delta(game.ball.z, enemy_goal_line);
        double dx = delta(game.ball.x, 0);
        double alfa = Math.toDegrees(90);
        if (dx != 0) {
            alfa = Math.atan(dz / dx);
        }
        double b_r_dx = (me.radius / 2 + game.ball.radius) * Math.cos(alfa);
        double b_r_dz = (me.radius / 2 + game.ball.radius) * Math.sin(alfa);

        if (DEBUG_ON) {
            System.out.printf(" alfa:%5.2f dx:%5.2f dz:%5.2f b_r_dx:%5.2f b_r_dz:%5.2f", Math.toDegrees(alfa), dx, dz, b_r_dx, b_r_dz);
        }
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format(" alfa:%5.2f dx:%5.2f dz:%5.2f b_r_dx:%5.2f b_r_dz:%5.2f", Math.toDegrees(alfa), dx, dz, b_r_dx, b_r_dz);
        }

        if (alfa > 0) {
            pos.x = -dx - b_r_dx;
            pos.y = me.radius;
            pos.z = enemy_goal_line - dz - b_r_dz;
        } else {
            pos.x = -dx + b_r_dx;
            pos.y = me.radius;
            pos.z = enemy_goal_line - dz + b_r_dz;
        }

        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format(" pos.x:%5.2f pos.z:%5.2f |bestAttackCoords** \n", pos.x, pos.z);
        }
        if (DEBUG_ON) {
            System.out.printf(" pos.x:%5.2f pos.z:%5.2f |bestAttackCoords** \n", pos.x, pos.z);
        }
        return pos;
    }

    /***********************************************/
    Pos moveAntiClockWise(final Robot me, final Rules rules, final Game game, final Action action) { //обход против часовой стрелки следующая точка.
        if (DEB_ON_SCR) {
            strToScr = strToScr + "\n***moveAntiClockWise|";
        }

        Pos pos = new Pos();
//    double MAX_RADIUS_ATTACK=rules.BALL_RADIUS+rules.ROBOT_RADIUS+10;

        if (DEBUG_ON) {
            System.out.print("***testbeg");
        }
        double dx = delta(me.x, game.ball.x);
        double dy = me.radius;
        double dz = delta(me.z, game.ball.z);

        double alfa0 = Math.toRadians(90);
        if (dz != 0) {
            alfa0 = Math.atan(dx / dz);
        }
        double R = Math.sqrt(dx * dx + dz * dz);
        double S = rules.ROBOT_MAX_GROUND_SPEED / rules.TICKS_PER_SECOND;
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format("dx:%5.2f|dy:%5.2f|dz%5.2f", dx, dy, dz);
        }

        double alfa = 0;

        if ((S / (2 * R) <= 1) && (R != 0)) {
            alfa = Math.asin(S / (2 * R));
        }
        if (DEBUG_ON) {
            System.out.printf(" test: dx:%6.2f", dx);
            System.out.printf(" dz:%6.2f", dz);
        }
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format(" R=%5.2f|S=%5.2f|Alfa=%5.2f|Alfa0=%5.2f", R, S, Math.toDegrees(alfa), Math.toDegrees(alfa0));
        }

        pos.x = game.ball.x + R * Math.sin(alfa + alfa0);
        pos.z = game.ball.z + R * Math.cos(alfa + alfa0);

        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format(" posx=%5.2f|posz=%5.2f", pos.x, pos.z);
            strToScr = strToScr + "***";
        }
        return pos;
    }

    /***********************************************/
    Pos moveAntiClockWise2(final Robot me, final Rules rules, final Game game, final Action action, Pos point_pos) { //обход против часовой стрелки точки с заданными координатами pos
        if (DEB_ON_SCR) {
            strToScr = strToScr + "\n**moveAntiClockWise2|";
            strToScr = strToScr + String.format("me.x:%5.2f|y:%5.2f|z%5.2f", me.x, me.y, me.z);
            strToScr = strToScr + String.format("point_pos.x:%5.2f|py:%5.2f|pz%5.2f", point_pos.x, point_pos.y, point_pos.z);
        }

        Pos pos = new Pos();
//    double MAX_RADIUS_ATTACK=rules.BALL_RADIUS+rules.ROBOT_RADIUS+10;

        if (DEBUG_ON) {
            System.out.print("***testbeg");
        }
        double dx = delta(me.x, point_pos.x);
        double dy = me.radius;
        double dz = delta(me.z, point_pos.z);

        double alfa0 = Math.toRadians(90);
        if (dz != 0) {
            alfa0 = Math.atan(dx / dz);
        }
        double R = Math.sqrt(dx * dx + dz * dz);
        double S = rules.ROBOT_MAX_GROUND_SPEED / rules.TICKS_PER_SECOND;
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format("dx:%5.2f|dy:%5.2f|dz%5.2f", dx, dy, dz);
        }

        double alfa = 0;

        if ((S / (2 * R) <= 1) && (R != 0)) {
            alfa = Math.asin(S / (2 * R));
        }
        if (DEBUG_ON) {
            System.out.printf(" test: dx:%6.2f", dx);
            System.out.printf(" dz:%6.2f", dz);
        }
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format(" R=%5.2f|S=%5.2f|Alfa=%5.2f|Alfa0=%5.2f", R, S, Math.toDegrees(alfa), Math.toDegrees(alfa0));
        }
        pos.x = game.ball.x + R * Math.sin(alfa + alfa0);
        pos.z = game.ball.z + R * Math.cos(alfa + alfa0);
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format(" posx=%5.2f|posz=%5.2f moveAntiClockWise2**", pos.x, pos.z);
        }

        return pos;
    }

    /***********************************************/
    Pos moveClockWise(final Robot me, final Rules rules, final Game game, final Action action) { //обход по чпасовой стрелке следующая точка.
        if (DEB_ON_SCR) {
            strToScr = strToScr + "\n***moveClockWise|";
        }
        Pos pos = new Pos();
//        double MAX_RADIUS_ATTACK=rules.BALL_RADIUS+rules.ROBOT_RADIUS+10;

        if (DEBUG_ON) {
            System.out.print("***testbeg");
        }
        double dx = delta(me.x, game.ball.x);
        double dy = me.radius;
        double dz = delta(me.z, game.ball.z);

        double alfa0 = Math.toRadians(90);
        if (dz != 0) {
            alfa0 = Math.atan(dx / dz);
        }
        double R = Math.sqrt(dx * dx + dz * dz);
        double S = rules.ROBOT_MAX_GROUND_SPEED / rules.TICKS_PER_SECOND;
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format("dx:%5.2f|dy:%5.2f|dz%5.2f", dx, dy, dz);
        }

        double alfa = 0;

        if ((S / (2 * R) <= 1) && (R != 0)) {
            alfa = Math.asin(S / (2 * R));
        }
        if (DEBUG_ON) {
            System.out.printf(" test: dx:%6.2f", dx);
            System.out.printf(" dz:%6.2f", dz);
        }
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format(" R=%5.2f|S=%5.2f|Alfa=%5.2f|Alfa0=%5.2f", R, S, Math.toDegrees(alfa), Math.toDegrees(alfa0));
        }

        pos.x = game.ball.x + R * Math.sin(alfa0 - alfa);
        pos.z = game.ball.z + R * Math.cos(alfa0 - alfa);
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format(" posx=%5.2f|posz=%5.2f", pos.x, pos.z);
            strToScr = strToScr + "***";
        }
        return pos;
    }

    /***********************************************/
    Pos moveClockWise2(final Robot me, final Rules rules, final Game game, final Action action, Pos point_pos) { //обход по чпасовой стрелке следующая точка.strToScr = strToScr+"\n***moveClockWise|";
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format("me.x:%5.2f|y:%5.2f|z%5.2f", me.x, me.y, me.z);
            strToScr = strToScr + String.format("point_pos.x:%5.2f|py:%5.2f|pz%5.2f", point_pos.x, point_pos.y, point_pos.z);
        }
        Pos pos = new Pos();
//        double MAX_RADIUS_ATTACK=rules.BALL_RADIUS+rules.ROBOT_RADIUS+10;

        if (DEBUG_ON) {
            System.out.print("***testbeg");
        }
        double dx = delta(me.x, point_pos.x);
        double dy = me.radius;
        double dz = delta(me.z, point_pos.z);

        double alfa0 = Math.toRadians(90);
        if (dz != 0) {
            alfa0 = Math.atan(dx / dz);
        }
        double R = Math.sqrt(dx * dx + dz * dz);
        double S = rules.ROBOT_MAX_GROUND_SPEED / rules.TICKS_PER_SECOND;
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format("dx:%5.2f|dy:%5.2f|dz%5.2f", dx, dy, dz);
        }

        double alfa = 0;

        if ((S / (2 * R) <= 1) && (R != 0)) {
            alfa = Math.asin(S / (2 * R));
        }
        if (DEBUG_ON) {
            System.out.printf(" test: dx:%6.2f", dx);
            System.out.printf(" dz:%6.2f", dz);
        }
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format(" R=%5.2f|S=%5.2f|Alfa=%5.2f|Alfa0=%5.2f", R, S, Math.toDegrees(alfa), Math.toDegrees(alfa0));
        }

        pos.x = game.ball.x + R * Math.sin(alfa0 - alfa);
        pos.z = game.ball.z + R * Math.cos(alfa0 - alfa);
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format(" posx=%5.2f|posz=%5.2f moveAntiClockWise2**", pos.x, pos.z);
        }
        return pos;
    }

    /***********************************************/
    Robot2 strateg_d01(final Robot me, final Rules rules, final Game game, final Action action) { // стратегия защита тупо стоять в точке ожидаемого пресечения мячом линии ворот

        Robot2 next_step = new Robot2(); // структура хранения результата
//          printInConsole(me,rules,game,action);

        CalculPoint calculballpoint = new CalculPoint();

        calculballpoint = TimeBallInGate(me, rules, game);
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format("**COUNTS:%d bl_x:%6.2f bl_z:%6.2f \n ", calculballpoint.tick, calculballpoint.x, calculballpoint.z);
        }

        Pos pos = new Pos();


        if (calculballpoint.tick >= -1) { // если есть перечечение или выше

            pos.x = calculballpoint.x;
            pos.y = calculballpoint.y;
            pos.z = calculballpoint.z;
            next_step = moveToPoint(me, rules, pos, calculballpoint.tick);
        } else {
            if (game.ball.x > rules.arena.goal_width / 2) {
                pos.x = rules.arena.goal_width / 2;
            } else {
                pos.x = Math.max(game.ball.x, -rules.arena.goal_width / 2);
            }

//            pos.x = 0;
            pos.y = 0;
//            pos.z = -rules.arena.depth/2+me.radius+rules.arena.bottom_radius;
            pos.z = -rules.arena.depth / 2 + me.radius/*+rules.arena.bottom_radius*/;


            next_step = moveToPoint(me, rules, pos, 1);
        }
        if (DEBUG_ON) {
            System.out.print("***timetojump ");
        }
        double jumptime = timeToJump(me, rules, game, action);
        if (DEBUG_ON) {
            System.out.print("timetojump***");
        }
        if (jumptime > 0) {
            next_step.act.jump_speed = jumptime;
        }

        return next_step;
    }

    /***********************************************/
    Robot2 strateg_d02(final Robot me, final Rules rules, final Game game, final Action action) { // стратегия защита тупо стоять в точке ожидаемого пресечения мячом линии ворот
        double GATE_LINE = -rules.arena.depth / 2 + me.radius + rules.arena.bottom_radius;
        Robot2 next_step = new Robot2(); // структура хранения результата
        next_step.rob = new Robot();
        next_step.act = new Action();

//        printInConsole(me,rules,game,action);

        CalculPoint calculballpoint = new CalculPoint();
        Object calculpoint = new Object();

        calculballpoint = TimeBallInGate(me, rules, game);
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format("**COUNTS:%d bl_x:%6.2f bl_z:%6.2f \n ", calculballpoint.tick, calculballpoint.x, calculballpoint.z);
        }

        Pos pos = new Pos();

        if (calculballpoint.tick >= -1) { // если есть пересечение или выше
            if (DEB_ON_SCR) {
                strToScr = strToScr + "#1";
            }
            pos.x = calculballpoint.x;
            pos.y = calculballpoint.y;
            pos.z = calculballpoint.z;
            next_step = moveToPoint(me, rules, pos, calculballpoint.tick);
        } else {
            if (DEB_ON_SCR) {
                strToScr = strToScr + "#2";
            }
            if (game.ball.x > rules.arena.goal_width / 2) {
                if (DEB_ON_SCR) {
                    strToScr = strToScr + "#3";
                }
                pos.x = rules.arena.goal_width / 2;
            } else {
                if (DEB_ON_SCR) {
                    strToScr = strToScr + "#4";
                }
                if (game.ball.x < -rules.arena.goal_width / 2) {
                    if (DEB_ON_SCR) {
                        strToScr = strToScr + "#5";
                    }
                    pos.x = -rules.arena.goal_width / 2;
                } else {
                    if (DEB_ON_SCR) {
                        strToScr = strToScr + "#6";
                    }
                    pos.x = game.ball.x;
                }
            }

//            pos.x = 0;
            pos.y = 0;
            pos.z = GATE_LINE;
            next_step = moveToPoint(me, rules, pos, 1);
        }
        if (DEBUG_ON) {
            System.out.print("***timetojump3");
        }
        double jumptime = timeToJump3(me, rules, game, action);
        if (DEBUG_ON) {
            System.out.print("timetojump3***");
        }
        if (jumptime > 0) {
            next_step.act.jump_speed = jumptime;
        }

        return next_step;
    }

    /***********************************************/
    Robot2 strateg_d03(final Robot me, final Rules rules, final Game game, final Action action) { // стратегия защита в точке ожидаемого пресечения мячом линии ворот и выходить их ворот

        ScrColor scrcolor = new ScrColor();
        Pos pos = new Pos();
        pos.x = game.current_tick % 40;
        pos.y = 2;
        pos.z = 10;
        scrcolor.r = 1;
        scrcolor.g = me.id;
        scrcolor.b = 0;
        scrcolor.a = 0.5;
        graphsToScr = graphsToScr + sphereToSCR(pos, 0.5, scrcolor);

        double GATE_LINE = -rules.arena.depth / 2 + me.radius + rules.arena.bottom_radius;
        Robot2 next_step = new Robot2(); // структура хранения результата
        next_step.rob = new Robot();
        next_step.act = new Action();
        Robot2 prom_step = new Robot2(); // структура хранения результата
        prom_step.rob = new Robot();
        prom_step.act = new Action();

//        printInConsole(me,rules,game,action);

        CalculPoint calculballpoint = new CalculPoint();
        Object calculpoint = new Object();

        calculballpoint = TimeBallInGate(me, rules, game); // расчет такта попадания мяча в ворота
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format("\n**d03   COUNTS:%d bl_x:%6.2f bl_z:%6.2f \n ", calculballpoint.tick, calculballpoint.x, calculballpoint.z);
        }
        if (DEBUG_ON) {
            System.out.printf("**d03   COUNTS:%d bl_x:%6.2f bl_z:%6.2f \n ", calculballpoint.tick, calculballpoint.x, calculballpoint.z);
        }


        next_step.count = -2;
        if (game.ball.z < MAX_OFFSET_FOR_DEFENCE) { // если мяч перешел на нашу сторону +MAX_OFFSET_FOR_DEFENCE
            if (DEB_ON_SCR) {
                strToScr = strToScr + " d03#0";
            }
            if (DEBUG_ON) {
                System.out.print(" d03#0");
            }
            prom_step = tryToPoint2(me, rules, game, action);
            if (prom_step.count > 0) {
                next_step = prom_step;
            }
        }
        if (next_step.count < 0) { // если мяч вне зоны досягаемости
            if (calculballpoint.tick >= -1) {  // если есть пересечение или выше
                if (DEB_ON_SCR) {
                    strToScr = strToScr + " d03#1";
                }
                if (DEBUG_ON) {
                    System.out.print(" d03#1");
                }

                if (calculballpoint.tick == 0) { // если мяч уже прошел оборону
                    // стараемся обежать
                    if (game.ball.x > 0) {
                        if (DEB_ON_SCR) {
                            strToScr = strToScr + " d03#2";
                        }
                        if (DEBUG_ON) {
                            System.out.print(" d03#2");
                        }
                        Pos need_pos = new Pos();
                        need_pos.x = game.ball.x;
                        need_pos.y = game.ball.y;
                        need_pos.z = game.ball.z;
                        pos = moveAntiClockWise2(me, rules, game, action, need_pos); // движение против часовой стрелки
                        next_step = moveToPoint(me, rules, pos, 0);

                    } else {
                        if (DEB_ON_SCR) {
                            strToScr = strToScr + " d03#3";
                        }
                        if (DEBUG_ON) {
                            System.out.print(" d03#3");
                        }
                        Pos need_pos = new Pos();
                        need_pos.x = game.ball.x;
                        need_pos.y = game.ball.y;
                        need_pos.z = game.ball.z;
                        pos = moveClockWise2(me, rules, game, action, need_pos); // движение против часовой стрелки
                        next_step = moveToPoint(me, rules, pos, 0);
                    }


                } else {
                    if (DEB_ON_SCR) {
                        strToScr = strToScr + " d03#4";
                    }
                    if (DEBUG_ON) {
                        System.out.print(" d03#4");
                    }
                    pos.x = calculballpoint.x;
                    pos.y = calculballpoint.y;
                    pos.z = calculballpoint.z;
                    next_step = moveToPoint(me, rules, pos, calculballpoint.tick);
                }
                if (DEB_ON_SCR) {
                    strToScr = strToScr + " d03#2";
                }
                if (DEBUG_ON) {
                    System.out.printf(" d03#2 Next_step.count=%d ", next_step.count);
                }

            }
            if (calculballpoint.tick < -1) { //если мяч не пересечет линию ворот
                if (DEB_ON_SCR) {
                    strToScr = strToScr + " d03#5";
                }
                if (DEBUG_ON) {
                    System.out.print(" d03#5");
                }
                if (game.ball.x > rules.arena.goal_width / 2) {
                    if (DEB_ON_SCR) {
                        strToScr = strToScr + " d03#6";
                    }
                    if (DEBUG_ON) {
                        System.out.print(" d03#6");
                    }
                    pos.x = rules.arena.goal_width / 2;
                } else {
                    if (DEB_ON_SCR) {
                        strToScr = strToScr + " d03#7";
                    }
                    if (DEBUG_ON) {
                        System.out.print(" d03#7");
                    }
                    if (game.ball.x < -rules.arena.goal_width / 2) {
                        if (DEB_ON_SCR) {
                            strToScr = strToScr + " d03#8";
                        }
                        if (DEBUG_ON) {
                            System.out.print(" d03#8");
                        }
                        pos.x = -rules.arena.goal_width / 2;
                    } else {
                        if (DEB_ON_SCR) {
                            strToScr = strToScr + " d03#9";
                        }
                        if (DEBUG_ON) {
                            System.out.print(" d03#9");
                        }
                        pos.x = game.ball.x;
                    }
                }

//            pos.x = 0;
                pos.y = 0;
                pos.z = GATE_LINE;
                next_step = moveToPoint(me, rules, pos, 1);
            }
            if (DEBUG_ON) {
                System.out.print(" d03***timetojump3");
            }
            double jumptime = timeToJump3(me, rules, game, action);
            if (DEBUG_ON) {
                System.out.print("timetojump3***d03");
            }

            if (jumptime > 0) {
                next_step.act.jump_speed = jumptime;
            }
        }
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format("d03 rbact_vx:%6.2f|rb_vy:%6.2f|rb_vz:%6.2f **d03\n ", next_step.act.target_velocity_x, next_step.act.target_velocity_y, next_step.act.target_velocity_z);
        }
        if (DEBUG_ON) {
            System.out.printf(" \n count:%2d next_step:rob_vx:%5.2f|vy:%5.2f|vz:%5.2f| **d03\n ", game.current_tick, next_step.rob.velocity_x, next_step.rob.velocity_y, next_step.rob.velocity_z);
        }

        if ((me.nitro_amount > 0) && (me.velocity_y < 0)) { // использование нитро
            next_step.act.target_velocity_y = -100;
            next_step.act.use_nitro = true;
        } else {
            next_step.act.use_nitro = false;
        }

        return next_step;
    }

    /***********************************************/
    Robot2 strateg_a02(final Robot me, final Rules rules, final Game game, final Action action) { //стратегия аткаа догонять мяч и толкать в сторону противника. Если перед мячом - обегать
        Pos pos = new Pos();
        if (GRAPH_ON_SCR) {
            ScrColor scrcolor = new ScrColor();

            pos.x = game.current_tick % 40;
            pos.y = 2;
            pos.z = 0;
            scrcolor.r = 1;
            scrcolor.g = me.id;
            scrcolor.b = 0;
            scrcolor.a = 0.5;
            graphsToScr = graphsToScr + sphereToSCR(pos, 0.5, scrcolor);
        }


        if (DEB_ON_SCR) {
            strToScr = strToScr + "\n**a02";
        }

        Robot2 next_step = new Robot2(); // структура хранения результата

        Object prom_step = new Object(); // структура хранения результата
        prom_step.pos = new Pos();
        prom_step.vel = new Vel();
        prom_step.acc = new Acc();
        prom_step.norm = new Normal();


        int c = tryToPoint(me, rules, game, action); // предсказание кол-ва тактов до встречи

        double MAX_RADIUS_ATTACK = rules.BALL_RADIUS + rules.ROBOT_RADIUS + DELTA_RADIUS_ATTACK;
        double ball_next_step_x = game.ball.x + game.ball.velocity_x / rules.TICKS_PER_SECOND;
        double ball_next_step_y = game.ball.y + game.ball.velocity_y / rules.TICKS_PER_SECOND;
        double ball_next_step_z = game.ball.z + game.ball.velocity_z / rules.TICKS_PER_SECOND;

        double delta_x = delta(me.x, ball_next_step_x);
        double delta_z = delta(me.z, ball_next_step_z);
        double vel_x = 0;
        double vel_z = 0;
        if (DEB_ON_SCR) {
            strToScr = strToScr + "#0";
        }
        double dx;
        double dz;
        dx = delta(me.x, game.ball.x);
        dz = delta(me.z, game.ball.z);
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format(" dx=%5.2f|dz=%5.2f|dist=%5.2f|MAXRAD=%5.2f", dx, dz, Math.sqrt(dx * dx + dz * dz), MAX_RADIUS_ATTACK);
        }

        if (me.z > game.ball.z) { // если мы впереди мяча пытаемся его обойти
            if (DEB_ON_SCR) {
                strToScr = strToScr + "#1";
            }

            if (Math.sqrt(dx * dx + dz * dz) > MAX_RADIUS_ATTACK) { // если далеко то догоняем
                if (DEB_ON_SCR) {
                    strToScr = strToScr + "#2";
                }
                pos.y = me.radius;
                pos.z = game.ball.z;
                if (game.ball.x < 0) {
                    if (DEB_ON_SCR) {
                        strToScr = strToScr + "#3";
                    }
                    pos.x = game.ball.x + me.radius + 1;
                } else {
                    if (DEB_ON_SCR) {
                        strToScr = strToScr + "#4";
                    }
                    pos.x = game.ball.x - me.radius - 1;
                }
                if (DEB_ON_SCR) {
                    strToScr = strToScr + String.format(" posx=%5.2f|posy=%5.2f|posz=%5.2f", pos.x, pos.y, pos.z);
                }
                next_step = moveToPoint(me, rules, pos, -1); // -1 -двигаться с макс. скоростью
            } else {
                // Если не очень далеко, начинаем обходить
                if (DEB_ON_SCR) {
                    strToScr = strToScr + "#5";
                }
                if (game.ball.x < 0) {
                    if (DEB_ON_SCR) {
                        strToScr = strToScr + "#6";
                    }
                    Pos need_pos = new Pos();
                    need_pos.x = game.ball.x;
                    need_pos.y = game.ball.y;
                    need_pos.z = game.ball.z;
                    pos = moveAntiClockWise2(me, rules, game, action, need_pos); // движение против часовой стрелки
                    if (DEB_ON_SCR) {
                        strToScr = strToScr + String.format(" posx=%5.2f|posy=%5.2f|posz=%5.2f", pos.x, pos.y, pos.z);
                    }
                } else {
                    if (DEB_ON_SCR) {
                        strToScr = strToScr + "#7";
                    }
                    Pos need_pos = new Pos();
                    need_pos.x = game.ball.x;
                    need_pos.y = game.ball.y;
                    need_pos.z = game.ball.z;
                    pos = moveClockWise2(me, rules, game, action, need_pos); // движение по часовой стрелке
                    if (DEB_ON_SCR) {
                        strToScr = strToScr + String.format(" posx=%5.2f|posy=%5.2f|posz=%5.2f", pos.x, pos.y, pos.z);
                    }
                }
                if (DEB_ON_SCR) {
                    strToScr = strToScr + "#8";
                    strToScr = strToScr + String.format(" posx=%5.2f|posy=%5.2f|posz=%5.2f", pos.x, pos.y, pos.z);
                }
                next_step = moveToPoint(me, rules, pos, -1); // -1 -двигаться с макс. скоростью


                if (DEBUG_ON) {
                    System.out.print("test end***");
                }

            }
            if (DEBUG_ON) {
                System.out.printf(" vel_x:%6.2f", vel_x);
                System.out.printf(" vel_z:%6.2f", vel_z);
            }

        } else { //если сзади то
            if (DEB_ON_SCR) {
                strToScr = strToScr + "#A";
            }
            if (game.ball.y < game.ball.radius + 2 * me.radius) {  // если мяч в воздухе
                if (DEB_ON_SCR) {
                    strToScr = strToScr + "#B";
                }
                pos = bestAttackCoords(me, rules, game);// Расчет координат точки вектора атаки
            } else {  // если мяч не в воздухе
                if (DEB_ON_SCR) {
                    strToScr = strToScr + "#C";
                }
                if (Math.sqrt(dx * dx + dz * dz) > MAX_RADIUS_ATTACK) // если далеко
                {
                    if (DEB_ON_SCR) {
                        strToScr = strToScr + "#D";
                    }
                    pos = bestAttackCoords(me, rules, game);// Расчет координат точки вектора атаки
                } else { // если сзади и близко
                    if (DEB_ON_SCR) {
                        strToScr = strToScr + "#E";
                    }
//                    pos.x = game.ball.x;
//                    pos.y = me.radius;
//                    pos.z = game.ball.z - game.ball.radius - me.radius;
                    pos = bestAttackCoords(me, rules, game);// Расчет координат точки вектора атаки
                }
            }

            next_step = moveToPoint(me, rules, pos, -1); // -1 -двигаться с макс. скоростью

        }

        double jumptime = timeToJump(me, rules, game, action);
        if (DEBUG_ON) {
            System.out.print("timetojump***");
        }
        if (jumptime > 0) {
            next_step.act.jump_speed = jumptime;
        }

        if ((me.nitro_amount > 0) && (me.velocity_y < 0)) { // использование нитро
            next_step.act.target_velocity_y = -100;
            next_step.act.use_nitro = true;
        } else {
            next_step.act.use_nitro = false;
        }

        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format("nst:vx=%5.2f|vy=%5.2f|vz=%5.2f|vs=%5.2f|jump:%5.2f ", next_step.act.target_velocity_x, next_step.act.target_velocity_y, next_step.act.target_velocity_z,
                    Math.sqrt(next_step.act.target_velocity_x * next_step.act.target_velocity_x + next_step.act.target_velocity_y * next_step.act.target_velocity_y
                            + next_step.act.target_velocity_z * next_step.act.target_velocity_z), jumptime);
            strToScr = strToScr + String.format("pos_x:%5.2f y:%5.2f z:%5.2f a02**\n", pos.x, pos.y, pos.z);
        }


        if (GRAPH_ON_SCR) {
            ScrColor scrcolor = new ScrColor();
            scrcolor.r = 1;
            scrcolor.g = me.id;
            scrcolor.b = 0;
            scrcolor.a = 0.5;
            graphsToScr = graphsToScr + sphereToSCR(pos, 0.5, scrcolor);
            Pos prom_pos = new Pos();
            //           prom_pos = pos;

            prom_pos.x = pos.x;
            prom_pos.y = rules.arena.height - 1;
            prom_pos.z = pos.z;


            graphsToScr = graphsToScr + sphereToSCR(prom_pos, 0.5, scrcolor);
            graphsToScr = graphsToScr + lineToSCR(pos, prom_pos, scrcolor, 5);
        }

        return next_step;
    }

    /***********************************************/
    Robot2 strateg_a01(final Robot me, final Rules rules, final Game game, final Action action) { //стратегия аткаа догонять мяч и толкать в сторону противника. Если перед мячом - обегать

        strToScr = strToScr + "\n**a01";

        Robot2 next_step = new Robot2(); // структура хранения результата

        Robot2 prom_step = new Robot2(); // структура хранения результата

        Pos pos = new Pos();

        int c = tryToPoint(me, rules, game, action); // предсказание кол-ва тактов до встречи
        double MAX_RADIUS_ATTACK = rules.BALL_RADIUS + rules.ROBOT_RADIUS + DELTA_RADIUS_ATTACK;
        double ball_next_step_x = game.ball.x + game.ball.velocity_x / rules.TICKS_PER_SECOND;
        double ball_next_step_y = game.ball.z + game.ball.velocity_y / rules.TICKS_PER_SECOND;
        double ball_next_step_z = game.ball.x + game.ball.velocity_z / rules.TICKS_PER_SECOND;

        action.target_velocity_y = 0;
        //           action.target_velocity_z=0;


        double delta_x = delta(me.x, ball_next_step_x);
        double delta_z = delta(me.z, ball_next_step_z);
        double vel_x = 0;
        double vel_z = 0;
        strToScr = strToScr + "#0";
        double dx;
        double dz;
        if (me.z > game.ball.z) { // если мы впереди мяча пытаемся его обойти
            strToScr = strToScr + "#1";
            dx = delta(me.x, game.ball.x);
            dz = delta(me.z, game.ball.z);

            if (Math.sqrt(dx * dx + dz * dz) > MAX_RADIUS_ATTACK) { // если далеко то догоняем
                strToScr = strToScr + "#2";
                pos.x = game.ball.x;
                pos.y = me.radius;
                pos.z = game.ball.z;
                next_step = moveToPoint(me, rules, pos, -1); // -1 -двигаться с макс. скоростью
            } else {
                // Если не очень далеко, начинаем обходить
                strToScr = strToScr + "#3";
                if (game.ball.x < 0) {
                    pos = moveAntiClockWise(me, rules, game, action); // движение против часовой стрелки
                } else {
                    pos = moveClockWise(me, rules, game, action); // движение по часовой стрелке
                }
                strToScr = strToScr + "#4";
                next_step = moveToPoint(me, rules, pos, -1); // -1 -двигаться с макс. скоростью


                if (DEBUG_ON) {
                    System.out.print("test end***");
                }

            }
            if (DEBUG_ON) {
                System.out.printf(" vel_x:%6.2f", vel_x);
                System.out.printf(" vel_z:%6.2f", vel_z);
            }

//            next_step.vel.vx=vel_x;
//            next_step.vel.vz=vel_z;

/*
            if (delta_x<0) {
                next_step.vel.vx = vel_x;
            } else {
                next_step.vel.vx = -vel_x;
            }
*/
//            next_step.vel.vz = -vel_z;


        } else { //если сзади то
            strToScr = strToScr + "#6";
            pos.x = game.ball.x;
            pos.y = me.radius;
            pos.z = game.ball.z;
            next_step = moveToPoint(me, rules, pos, -1); // -1 -двигаться с макс. скоростью
        }
/*
                if (DEBUG_ON) {
                    if (game.current_tick<200) {
                        action.target_velocity_z = 100;
                        action.target_velocity_x = 0;
                    } else {
                        if (DEBUG_ON) {
                            System.out.print("Hello");
                    }
                }

            }
*/
//        action.target_velocity_x=0;
//        action.target_velocity_y=0;
//        action.target_velocity_z=0;
        strToScr = strToScr + String.format("nst:vx=%5.2f|vy=%5.2f|vz=%5.2f|vs=%5.2f|jump:%5.2f a01**\n", next_step.rob.velocity_x, next_step.rob.velocity_y, next_step.rob.velocity_z,
                Math.sqrt(next_step.rob.velocity_x * next_step.rob.velocity_x + next_step.rob.velocity_y * next_step.rob.velocity_y + next_step.rob.velocity_z * next_step.rob.velocity_z));

        return next_step;
    }

    /***********************************************/
    boolean bBeginRound(Robot me, Rules rules, Game game, Action action)   //проверка на начало раунда
    {
        boolean bbegin_round = false;
        if ((game.ball.x == 0) && (game.ball.z == 0)) {     // проверка на начало текущего раунда
            bbegin_round = true;
            int i = 0;
            while (bbegin_round && (i < rules.team_size * 2)) {
                if ((game.robots[i].velocity_x == 0) && (game.robots[i].velocity_y == 0) && (game.robots[i].velocity_z == 0) && (game.robots[i].y == rules.ROBOT_RADIUS) && ((game.robots[i].touch))) {
                } else {
                    bbegin_round = false;
                }
                i++;
            }
            if (bbegin_round) {
                ticks_per_round = game.current_tick;
            }
        }
        return bbegin_round;

    }

    ;

    /***********************************************/
    Robot2 beginRound(final Robot me, final Rules rules, final Game game, final Action action)  // алгоритм начала раунда
    {
        strToScr += "\n **beginRound ";
        if (DEBUG_ON) {
            System.out.print("***beginRound");
        }

        Robot2 next_step = new Robot2(); // структура хранения результата
        next_step.rob = new Robot();
        next_step.act = new Action();

        Pos pos = new Pos();
        pos.x = game.ball.x;
        pos.y = game.ball.y;
        pos.z = game.ball.z;
        strToScr = strToScr + String.format("pos.x:%5.2f|y:%5.2f|z:5.2f", pos.x, pos.y, pos.z);
        next_step = moveToPoint(me, rules, pos, -1); // двигаться с макс. скоростью
        strToScr = strToScr + String.format("nst:vx=%5.2f|vy=%5.2f|vz=%5.2f|vs=%5.2f", next_step.act.target_velocity_x, next_step.act.target_velocity_y, next_step.act.target_velocity_z,
                Math.sqrt(next_step.act.target_velocity_x * next_step.act.target_velocity_x + next_step.act.target_velocity_y * next_step.act.target_velocity_y + next_step.act.target_velocity_z * next_step.act.target_velocity_z));

        double jumptime = timeToJump2(me, rules, game, action);

        if (jumptime > 0) {
            next_step.act.jump_speed = jumptime;
        }

        strToScr = strToScr + String.format("jump=%5.2f beginRound**\n", next_step.act.jump_speed);
        if (DEBUG_ON) {
            System.out.println("beginRound***");
        }
        return next_step;

    }

    /***********************************************/
    @Override
    public void act(final Robot me, final Rules rules, final Game game, final Action action) {

        graphsToScr = "";
//        System.out.println("t:"+ game.current_tick+ " id:"+ me.id+ " count:"+counter);
        graphsToScr = graphsToScr + textToSCR(String.format("t:%d id:%d count:%d", game.current_tick, me.id, counter));

        long startTickTime = System.nanoTime();

        Robot2 obj = new Robot2();
        obj.act = new Action();
        obj.rob = new Robot();
        obj.rob.velocity_x = 0;
        obj.rob.velocity_y = 0;
        obj.rob.velocity_z = 0;

        if (counter == 0) {
            System.out.println("RussianAICUP: \n" + VERSION);
            System.out.printf("DBG=%b|DBGSCR=%b|DLTRDSATTCK=%2.1f|DFNDPRDCTPNTS=%d|MXOFFSTDEFENCE=%d ", DEBUG_ON, DEB_ON_SCR, DELTA_RADIUS_ATTACK, DEFEND_PREDICT_POINTS, MAX_OFFSET_FOR_DEFENCE);
            Initial(me, rules, game, action);
            initial = true;
            ticks_per_round = 0;
            startfulltime = System.nanoTime();
            team_size = rules.team_size;
            for (int i = 0; i < rules.team_size; i++) {
                graph_current_string[i] = textToSCR("begin");
            }

        }
        counter = counter + 1;
        strToScr = "" + counter + "++" + me.id + "++";


//if (game.current_tick == 2) {
        //System.exit(55);
//}

        if (game.ball.z > 0) { // процент владения мячом
            full_attack_time++;
        }

        double pos_z = 100;
        for (int i = 0; i < rules.team_size * 2; i++) {
            if ((game.robots[i].z < pos_z) && (game.robots[i].is_teammate)) {
                defender = game.robots[i].id;
                pos_z = game.robots[i].z;
            }
        }
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format("Defender=%1d\n", defender);
        }

        boolean bbeginround = bBeginRound(me, rules, game, action);
        if (bbeginround) { // Если начало раунда
            ticks_per_round = game.current_tick;
            if (DEBUG_ON) {
                System.out.println("beginRound:" + game.current_tick);
            }
        }
        if ((game.current_tick - ticks_per_round) <= 50) { //
            obj = beginRound(me, rules, game, action);
        }

        printInConsole(me, rules, game, action);
        //System.out.print("bbb");

        if (counter % rules.team_size == 0) { // если начало такта
//            System.out.println("predict:"+ counter);
            predictBall(me, rules, game, action); // предсказание положения мяча
        }


        robots_arr[me.player_id] = me;
        if (!IsGoal(game, rules.arena)) { // если не гол


// Attacker
            if ((me.id != defender) && ((game.current_tick - ticks_per_round) > 50)) {   //  **********************
                if (DEB_ON_SCR) {
                    strToScr = strToScr + "Attack";
                }
                if (DEBUG_ON) {
                    System.out.println("Attack");
                }
                obj = strateg_a02(me, rules, game, action);
            } else {

            }

// Defender

            if ((me.id == defender) && ((game.current_tick - ticks_per_round) > 50)) {
                if (DEB_ON_SCR) {
                    strToScr = strToScr + "Defend";
                }
                if (DEBUG_ON) {
                    System.out.println("Defend");
                }
                obj = strateg_d03(me, rules, game, action);
            }
        }
//        action.target_velocity_x =obj.rob.velocity_x;
//        action.target_velocity_y =obj.rob.velocity_y;
//        action.target_velocity_z =obj.rob.velocity_z;
        /*
        if (obj.act.jump_speed>0){
            action.jump_speed = obj.act.jump_speed;
        } else {
            action.jump_speed = 0;
        }
        */


        action.target_velocity_x = obj.act.target_velocity_x;
        action.target_velocity_y = obj.act.target_velocity_y;
        action.target_velocity_z = obj.act.target_velocity_z;
        action.use_nitro = obj.act.use_nitro;
        action.jump_speed = obj.act.jump_speed;


// отладка
        if (DEBUG_ON) {
            System.out.println("отладка111");
        }
        if (/*(me.id==1)&&*/(DEBUG_ON)) {
            System.out.println("отладка222");

//            action.target_velocity_x = 0;
//            action.target_velocity_y = 0;
//            action.target_velocity_z = 0;
            if (game.current_tick <= 100) {
                //action.jump_speed = 1000;

                action.target_velocity_z = 0;
                action.target_velocity_x = 0;
                System.out.println("velmax");
            }
            if (game.current_tick > 20) {
                if (me.id == 1) {
                    action.target_velocity_y = 100;
                    action.jump_speed = 100;
                    action.use_nitro = true;
                }
                if (me.id == 2) {
                    action.target_velocity_y = 100;
                    action.jump_speed = 100;
                    action.use_nitro = false;
                }

            }
/*
            if ((game.current_tick > 20)) {
//                action.jump_speed = 10;
                  action.target_velocity_z = 0;
                System.out.println("vestop");
            }
*/


        }
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format("act_vx:%5.2f|vy:%5.2f}vz:%5.2f|vxz:%5.2f|jmpsp:%5.2f|bnitro:%b", action.target_velocity_x, action.target_velocity_y, action.target_velocity_z,
                    Math.sqrt(action.target_velocity_x * action.target_velocity_x + action.target_velocity_z * action.target_velocity_z), action.jump_speed, action.use_nitro);
        }
        if (DEBUG_ON) {
            System.out.printf("act_vx:%5.2f|vy:%5.2f}vz:%5.2f|vxz:%5.2f|jmpsp:%5.2f|bnitro:%b", action.target_velocity_x, action.target_velocity_y, action.target_velocity_z,
                    Math.sqrt(action.target_velocity_x * action.target_velocity_x + action.target_velocity_z * action.target_velocity_z), action.jump_speed, action.use_nitro);
        }

        System.out.println("nitro_accel:" + rules.ROBOT_NITRO_ACCELERATION);
        System.out.println("nitro_accel:" + rules.ROBOT_NITRO_ACCELERATION);


        for (int i = 1; i <= rules.team_size * 2; i++) {
            prevstrings[i - 1] = prevstrings[i];
        }
        if (DEB_ON_SCR) {
            strToScr2 = String.format("\n prev tact: (%d) id=%d ", game.current_tick, me.id);
            strToScr2 = strToScr2 + String.format("act_vx:%5.2f|vy:%5.2f}vz:%5.2f|vxz:%5.2f|jmpsp:%5.2f|bnitro:%b", action.target_velocity_x, action.target_velocity_y, action.target_velocity_z,
                    Math.sqrt(action.target_velocity_x * action.target_velocity_x + action.target_velocity_z * action.target_velocity_z), action.jump_speed, action.use_nitro);
            prevstrings[rules.team_size * 2] = strToScr2;

            strToScr = strToScr + "\n";
            for (int i = 1; i < rules.team_size + 1; i++) {
                strToScr += prevstrings[i];
                if (DEBUG_ON) {
                    System.out.println("array:" + i + ":" + prevstrings[i]);
                }
            }
        }

        act_tick++;
        long time_cur_tick = System.nanoTime() - startTickTime;
        if (time_longest_tick < time_cur_tick) {
            time_longest_tick = time_cur_tick;
            longest_tick = game.current_tick;
        }
        sumtime = sumtime + time_cur_tick;
        if (DEB_ON_SCR) {
            strToScr = strToScr + String.format("\nlngsttmtck=%5.3f|lngsttck=%d|sumtime=%5.3f|attck=%2.2f%%\nact**", 1.0 * time_longest_tick / 1000000000, longest_tick, 1.0 * sumtime / 1000000000,
                    1.0 * full_attack_time / game.current_tick * 100 / rules.team_size);
        }
        if (DEBUG_ON) {
            System.out.printf("\nlngsttmtck=%5.3f|lngsttck=%d|sumtime=%5.3f|attck=%2.2f%%\nact**", 1.0 * time_longest_tick / 1000000000, longest_tick, 1.0 * sumtime / 1000000000,
                    1.0 * full_attack_time / game.current_tick * 100 / rules.team_size);
        } else {

            if (game.current_tick % 2000 == 0 && !bool_time_tick && game.current_tick > 0) {
                System.out.printf("\nT:%5d|lngsttm=%5.3f|tck=%d|stime=%5.3f|attck=%2.1f%% *", game.current_tick, 1.0 * time_longest_tick / 1000000000, longest_tick, 1.0 * sumtime / 1000000000,
                        1.0 * full_attack_time / game.current_tick * 100 / rules.team_size);
                bool_time_tick = true;
            } else {
                bool_time_tick = false;
            }
        }

        if (GRAPH_ON_SCR) {
            String s = String.format("id:%d action.vx:%5.2f vy:%5.2f vz:%5.2f vxy:%5.2f vxyz:%5.2f jump_sp:%5.2f nitro:%b", me.id, action.target_velocity_x, action.target_velocity_y, action.target_velocity_z,
                    Math.sqrt(action.target_velocity_x * action.target_velocity_x + action.target_velocity_z * action.target_velocity_z),
                    Math.sqrt(action.target_velocity_x * action.target_velocity_x + action.target_velocity_y * action.target_velocity_y + action.target_velocity_z * action.target_velocity_z),
                    action.jump_speed, action.use_nitro);
            graphsToScr = graphsToScr + textToSCR(s);
        }

        counter2 = counter2 + 1;
        graph_current_string[counter % rules.team_size] = graphsToScr;
    }


    @Override
    public String customRendering() {
        String s2 = "";
        for (int i = 0; i < team_size; i++) {
            s2 += graph_current_string[i];
//            System.out.println("i:"+i+":"+s2);
        }
//        s2 = s2 + graphsToScr;
//        s2 = s2+ sphereToSCR(pos1,2.0,scrColor)+",";
//        s2 =s2 +lineToSCR(pos1,pos2,scrColor,5)+",";
        s2 = s2 + textToSCR("+" + counter + "+" + strToScr);
        int end_str = s2.length();
        s2 = s2.substring(0, end_str - 1);

        s2 = "[" + s2 + "]";
//        System.out.println("s2:*"+s2+"*");
//        s = strToScr + s2;
        //s =s2;

//        System.out.println("Render. count:" + counter +"counter2:" +counter2 );
        return s2;

    }

    String lineToSCR(Pos pos1, Pos pos2, ScrColor color, double width) {
        String s;
        Line line = new Line();
        line.x1 = 0.01 * Math.round(pos1.x * 100);
        line.y1 = 0.01 * Math.round(pos1.y * 100);
        line.z1 = 0.01 * Math.round(pos1.z * 100);
        line.x2 = 0.01 * Math.round(pos2.x * 100);
        line.y2 = 0.01 * Math.round(pos2.y * 100);
        line.z2 = 0.01 * Math.round(pos2.z * 100);
        line.r = color.r;
        line.g = color.g;
        line.b = color.b;
        line.a = color.a;
        line.width = width;
        Gson gson = new Gson();
        s = "{\"Line\":" + gson.toJson(line) + "},";
//        System.out.println("s:"+s);
        return s;
    }
    String sphereToSCR(Pos pos1, double radius, ScrColor color) {
        String s;
        Sphere sphere = new Sphere();
        sphere.x = 0.01 * Math.round(pos1.x * 100);
        sphere.y = 0.01 * Math.round(pos1.y * 100);
        sphere.z = 0.01 * Math.round(pos1.z * 100);
        sphere.radius = radius;
        sphere.r = color.r;
        sphere.g = color.g;
        sphere.b = color.b;
        sphere.a = color.a;
        Gson gson = new Gson();
        s = "{\"Sphere\": " + gson.toJson(sphere) + "},";
//        System.out.println("s:"+s);
        return s;
    }

    String textToSCR(String s) {
        Gson gson = new Gson();
        //        System.out.println("s:"+s2);
        return "{\"Text\": " + gson.toJson(s) + "},";
    }

    static class ScrColor {
        double r;
        double g;
        double b;
        double a;
    }

    static class Line {
        public double x1;
        public double y1;
        public double z1;
        public double x2;
        public double y2;
        public double z2;
        public double width;
        public double r;
        public double g;
        public double b;
        public double a;
    }

    static class Sphere {
        public double x;
        public double y;
        public double z;
        public double radius;
        public double r;
        public double g;
        public double b;
        public double a;
    }

}