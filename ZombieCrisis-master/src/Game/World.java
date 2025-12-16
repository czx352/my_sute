package Game;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class World {
    private CopyOnWriteArrayList<GameObject> objects;
    private CopyOnWriteArrayList<Blood> bloods;
    private List<Box> pickedBoxes;
    private int maxBloodNum = 5000;
    private int bloodNum;
    private int width;
    private int height;
    private int maxEnemyNum;
    private int currentEnemyNum;
    private int producedEnemyNum;
    private int produceDelay;
    private int boxDelay;
    private Image endImg;
    private int end;

    // 新增：分数和模式相关字段
    private int totalScore; // 累计分数
    private boolean isDoublePlayer; // 是否为双人模式（true=双人，false=单人）


    public World(int width, int height, boolean doublePlayer) {
        this.width = width;
        this.height = height;
        this.objects = new CopyOnWriteArrayList<>();
        this.bloods = new CopyOnWriteArrayList<>();
        this.pickedBoxes = new ArrayList<>();
        this.bloodNum = 0;
        this.maxEnemyNum = 3;
        this.currentEnemyNum = 0;
        this.producedEnemyNum = 0;
        this.boxDelay = 0;
        this.end = -1;
        this.isDoublePlayer = doublePlayer; // 初始化模式
        this.totalScore = 0; // 初始化分数

        // 加载游戏结束图片
        try {
            endImg = ImageIO.read(World.class.getClassLoader().getResourceAsStream("images/gameover.png"));
        } catch (IOException e) {
            e.printStackTrace();
            endImg = Toolkit.getDefaultToolkit().getImage(World.class.getClassLoader().getResource("images/gameover.png"));
        }

        // 初始化游戏对象
        objects.add(new Hero(340, 180, 1, this));
        if (doublePlayer)
            objects.add(new Hero(620, 180, 0, this));
        objects.add(new Border(0, this));
        objects.add(new Border(1, this));
        objects.add(new Border(2, this));
        objects.add(new Border(3, this));
        objects.add(new Box(320, 360, this));
        objects.add(new Box(640, 360, this));
        for (int i = 1; i <= 2; i++)
            for (int j = 1; j <= 3; j++) {
                objects.add(new Wall(width / 4 * j, height / 3 * i, this));
            }
    }

    // 新增：加分方法（击杀敌人等场景调用）
    public void addScore(int points) {
        this.totalScore += points;
    }

    // 新增：保存分数到数据库
    public void saveScoreToDB() {
        String sql = "INSERT INTO scores (score, mode, create_time) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, this.totalScore); // 分数
            pstmt.setInt(2, isDoublePlayer ? 2 : 1); // 模式：1=单人，2=双人
            pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis())); // 当前时间
            pstmt.executeUpdate();
            System.out.println("分数保存成功：" + totalScore + "分（" + (isDoublePlayer ? "双人" : "单人") + "模式）");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("分数保存失败：" + e.getMessage());
        } finally {
            DBUtil.close(pstmt, conn); // 关闭资源
        }
    }

    public Iterator<GameObject> getObjectsIterator() {
        return objects.iterator();
    }

    public void removeObject(GameObject obj) {
        objects.remove(obj);
    }

    public void addObject(GameObject obj) {
        objects.add(obj);
    }

    public GameObject getObject(int index) {
        return objects.get(index);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getCurrentEnemyNum() {
        return currentEnemyNum;
    }

    public void setCurrentEnemyNum(int currentEnemyNum) {
        this.currentEnemyNum = currentEnemyNum;
    }

    public void setProduceDelay() {
        this.produceDelay = 50;
    }

    public void pickUpBox(Box box) {
        this.removeObject(box);
        pickedBoxes.add(box);
        box.setDelay(Box.DELAYTIME);
    }

    public void produceBox() {
        for (Box box : pickedBoxes) {
            if (box.getDelay() == 0) {
                this.addObject(box);
                pickedBoxes.remove(box);
                break;
            } else {
                box.setDelay(box.getDelay() - 1);
            }
        }
    }

    public void produceEnemy() {
        produceDelay = (produceDelay - 1 > 0 ? produceDelay - 1 : 0);
        if (currentEnemyNum >= 100) return;
        if (producedEnemyNum < maxEnemyNum && produceDelay <= 0) {
            Random rand = new Random();
            int pos = Math.abs(rand.nextInt()) % 4;
            int type = Math.abs(rand.nextInt()) % 100;
            int t = (rand.nextInt() % 2) * Role.PICOFFSET * 2;
            int off = Role.PICOFFSET + 10;
            switch (pos) {
                case 0:
                    objects.add(type < 10 ? new Ghost(width / 2 + t, off, this) : new Monster(width / 2 + t, off, this));
                    break;
                case 1:
                    objects.add(type < 10 ? new Ghost(off, height / 2 + t, this) : new Monster(off, height / 2 + t, this));
                    break;
                case 2:
                    objects.add(type < 10 ? new Ghost(width / 2 + t, height - off, this) : new Monster(width / 2 + t, height - off, this));
                    break;
                case 3:
                    objects.add(type < 10 ? new Ghost(width - off, height / 2 + t, this) : new Monster(width - off, height / 2 + t, this));
                    break;
                default:
                    break;
            }
            currentEnemyNum++;
            producedEnemyNum++;
            setProduceDelay();
        } else if (currentEnemyNum <= 0 && producedEnemyNum == maxEnemyNum) {
            maxEnemyNum += 3;
            producedEnemyNum = 0;
        }
    }

    public boolean collisionDetection(GameObject obj) {
        Iterator<GameObject> iter = this.getObjectsIterator();
        int flag = 0;
        while (iter.hasNext()) {
            GameObject tmpObj = iter.next();
            if (!obj.equals(tmpObj) && tmpObj.getHP() > 0) {
                if (obj.collisionDetection(tmpObj)) {
                    obj.collisionResponse(tmpObj);
                    flag = 1;
                }
            }
        }
        return flag == 1;
    }

    public void objectSort() {
        Collections.sort(objects, new Comparator<GameObject>() {
            @Override
            public int compare(GameObject obj1, GameObject obj2) {
                int i = obj1.getY() - obj2.getY();
                if (i == 0) {
                    return obj1.getX() - obj2.getX();
                }
                return i;
            }
        });
    }

    public void drawWorld(Graphics g) {
        if (isEnd()) {
            end--;
        }
        produceEnemy();
        produceBox();
        for (Blood blood : bloods) {
            blood.draw(g);
        }
        this.objectSort();
        Iterator<GameObject> iter = this.getObjectsIterator();
        while (iter.hasNext()) {
            iter.next().draw(g);
        }
    }

    public void objDead(Object obj) {
        if (obj instanceof Enemy) {
            currentEnemyNum--;
            // 新增：敌人死亡时加分（普通怪物10分，幽灵20分，可根据实际调整）
            if (obj instanceof Monster) {
                addScore(10);
            } else if (obj instanceof Ghost) {
                addScore(20);
            }
        }
        this.objects.remove(obj);
    }

    public int addBloodNum() {
        bloodNum = (bloodNum + 1) % maxBloodNum;
        return bloodNum;
    }

    public boolean searchHero() {
        Iterator<GameObject> iter = getObjectsIterator();
        while (iter.hasNext()) {
            GameObject obj = iter.next();
            if (obj instanceof Hero && obj.getHP() > 0) return true;
        }
        gameOver(); // 英雄全部死亡，游戏结束
        return false;
    }

    public void addBlood(int x, int y) {
        int n = addBloodNum();
        if (bloods.size() < maxBloodNum) {
            bloods.add(new Blood(x, y, this));
        } else {
            bloods.set(n, new Blood(x, y, this));
        }
    }

    public void drawEnd(Graphics g) {
        g.drawImage(endImg, 0, 0, width, height, null);
        // 新增：游戏结束时显示分数
        g.setColor(Color.RED);
        g.setFont(new Font("宋体", Font.BOLD, 40));
        String modeStr = isDoublePlayer ? "双人模式" : "单人模式";
        g.drawString("最终得分: " + totalScore + " （" + modeStr + "）", width / 2 - 200, height / 2);
    }

    public void gameOver() {
        if (!isEnd()) {
            this.end = 30;
            saveScoreToDB(); // 游戏结束时保存分数
        }
    }

    public boolean isEnd() {
        return this.end >= 0;
    }

    public boolean End() {
        return this.end == 0;
    }

    // 新增：获取当前分数（用于调试或显示）
    public int getTotalScore() {
        return totalScore;
    }
}