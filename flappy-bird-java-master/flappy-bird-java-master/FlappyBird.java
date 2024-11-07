import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;
import javax.sound.sampled.*;
import java.io.IOException;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {
    int boardWidth = 360;
    int boardHeight = 640;

    // รูปภาพ
    Image backgroundImg;
    Image backgroundImg1;
    Image backgroundImg2;
    Image backgroundImg3;
    Image backgroundImg4;
    Image backgroundImg5;
    Image birdImg;
    Image birdImg1; // นกตัวที่ 1
    Image birdImg2; // นกตัวที่ 2
    Image birdImg3; // นกตัวที่ 3
    Image topPipeImg;
    Image bottomPipeImg;
    Image explosionImg; // เพิ่มรูปภาพสำหรับเอฟเฟกต์ระเบิด

    // การตั้งค่าเวลานับถอยหลัง
    int countdownTime = 60; // เวลานับถอยหลังในวินาที (1 นาที)
    Timer countdownTimer;

    // ระดับความยาก
    String difficulty;
    int pipeSpeed; // ความเร็วของท่อ
    int gravity; // แรงโน้มถ่วง
    int jumpStrength; // พลังในการกระโดดของนก

    // ตัวแปรสำหรับ Combo
    int comboCount = 0;      // นับจำนวนท่อที่ผ่านติดต่อกัน
    int comboMultiplier = 1; // ตัวคูณคะแนนตามจำนวนท่อที่ผ่านติดต่อกัน
    boolean showComboEffect = false; // ตัวแปรสำหรับควบคุมแสงไฟกระพริบเมื่อทำคอมโบได้

    boolean backgroundChangedAt50 = false;
    boolean backgroundChangedAt100 = false;
    boolean backgroundChangedAt150 = false;
    boolean backgroundChangedAt200 = false;

    // ตัวแปรสำหรับระเบิด
    boolean showExplosion = false;
    int explosionX, explosionY;

    // เพลงประกอบ
    Clip backgroundMusic;

    // คลาสนก
    int birdX = boardWidth / 8;
    int birdY = boardWidth / 2;
    int birdWidth = 34;
    int birdHeight = 24;

    class Bird {
        int x = birdX;
        int y = birdY;
        int width = birdWidth;
        int height = birdHeight;
        Image img;

        Bird(Image img) {
            this.img = img;
        }
    }

    // คลาสท่อ
    int pipeX = boardWidth;
    int pipeY = 0;
    int pipeWidth = 64;  // ขนาดที่ถูกปรับลดลง 1/6
    int pipeHeight = 512;

    class Pipe {
        int x = pipeX;
        int y = pipeY;
        int width = pipeWidth;
        int height = pipeHeight;
        Image img;
        boolean passed = false;

        boolean movable; // ท่อสามารถขยับขึ้นลงได้หรือไม่
        int moveSpeed;   // ความเร็วในการขยับในแนวตั้ง

        Pipe(Image img, boolean movable) {
            this.img = img;
            this.movable = movable;
            this.moveSpeed = random.nextInt(5) + 3; // สุ่มความเร็ว
        }

        // ฟังก์ชันสำหรับการขยับท่อในแนวตั้ง
        void moveVertically() {
            if (movable) {
                this.y += moveSpeed;
                /// ตรวจสอบไม่ให้ท่อเคลื่อนที่ออกนอกจอ
                if (this.y <= 0 || this.y + this.height >= boardHeight) {
                    moveSpeed *= -1; // สลับทิศทาง
                }
            }
        }
    }

    // ลอจิกของเกม
    Bird bird;
    int velocityY = 0; // ความเร็วของนกในการเคลื่อนขึ้นหรือลง

    ArrayList<Pipe> pipes;
    Random random = new Random();

    Timer gameLoop;
    Timer placePipeTimer;
    boolean gameOver = false;
    double score = 0;

    // ตัวแปรสำหรับคะแนนสูงสุด
    double highScore = 0;

    // ตัวแปรสำหรับเสียงกระโดด
    Clip jumpSound;

    // ตัวแปรสำหรับเสียงการเปลี่ยนพื้นหลัง
    Clip backgroundChangeSound;

    FlappyBird() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);

        // เลือกระดับความยากผ่าน JOptionPane 
        String[] difficultyOptions = {"Easy", "Medium", "Hard"};
        difficulty = (String) JOptionPane.showInputDialog(null, "Select level", "Level", JOptionPane.QUESTION_MESSAGE, null, difficultyOptions, difficultyOptions[0]);

        // ตั้งค่าความเร็วและแรงโน้มถ่วงตามระดับความยากที่เลือก
        switch (difficulty) {
            case "Easy":
                pipeSpeed = -5;
                gravity = 1;
                jumpStrength = -10;
                break;
            case "Medium":
                pipeSpeed = -6;
                gravity = 2;
                jumpStrength = -13;
                break;
            case "Hard":
                pipeSpeed = -10;
                gravity = 3;
                jumpStrength = -15;
                break;
        }

        // โหลดรูปภาพนก
        birdImg1 = new ImageIcon(getClass().getResource("./bird1.png")).getImage();
        birdImg2 = new ImageIcon(getClass().getResource("./bird2.png")).getImage();
        birdImg3 = new ImageIcon(getClass().getResource("./bird3.png")).getImage();

        // ให้ผู้เล่นเลือกตัวละครนก
        String[] birdOptions = {"Bird 1", "Bird 2", "Bird 3"};
        String selectedBird = (String) JOptionPane.showInputDialog(null, "Select Bird", "Bird Selection", JOptionPane.QUESTION_MESSAGE, null, birdOptions, birdOptions[0]);

        // ตั้งค่ารูปนกตามที่เลือก
        switch (selectedBird) {
            case "Bird 1":
                birdImg = birdImg1;
                break;
            case "Bird 2":
                birdImg = birdImg2;
                break;
            case "Bird 3":
                birdImg = birdImg3;
                break;
        }

        // โหลดรูปภาพท่อและพื้นหลัง
        backgroundImg1 = new ImageIcon(getClass().getResource("./flappybirdbg.png")).getImage();
        backgroundImg2 = new ImageIcon(getClass().getResource("./flappybirdbg2.png")).getImage();
        backgroundImg3 = new ImageIcon(getClass().getResource("./flappybirdbg3.png")).getImage();
        backgroundImg4 = new ImageIcon(getClass().getResource("./flappybirdbg4.png")).getImage();
        backgroundImg5 = new ImageIcon(getClass().getResource("./flappybirdbg5.png")).getImage();
        topPipeImg = new ImageIcon(getClass().getResource("./toppipe.png")).getImage();
        bottomPipeImg = new ImageIcon(getClass().getResource("./bottompipe.png")).getImage();
        explosionImg = new ImageIcon(getClass().getResource("./explosion.png")).getImage(); // โหลดรูปภาพระเบิด

        // ตั้งค่า background เริ่มต้น
        backgroundImg = backgroundImg1;

        // สร้างนก
        bird = new Bird(birdImg);
        pipes = new ArrayList<Pipe>();

        // โหลดไฟล์เสียงกระโดด
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(getClass().getResource("/jump.wav"));
            jumpSound = AudioSystem.getClip();
            jumpSound.open(audioInputStream);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("Error loading jump sound: " + e.getMessage());
        }

        // โหลดไฟล์เสียงเปลี่ยนพื้นหลัง
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(getClass().getResource("/background_change.wav"));
            backgroundChangeSound = AudioSystem.getClip();
            backgroundChangeSound.open(audioInputStream);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("Error loading background change sound: " + e.getMessage());
        }

        // โหลดเพลงประกอบ
        try {
            AudioInputStream musicInputStream = AudioSystem.getAudioInputStream(getClass().getResource("/background_music.wav"));
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(musicInputStream);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY); // เล่นเพลงประกอบแบบลูป
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("Error loading background music: " + e.getMessage());
        }

        // ตั้งค่า timer สำหรับการวางท่อ
        placePipeTimer = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placePipes();
            }
        });
        placePipeTimer.start();

        // ตั้งค่าเกมลูป
        gameLoop = new Timer(1000 / 60, this);
        gameLoop.start();

        // ตั้งค่า timer สำหรับนับถอยหลัง
        countdownTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                countdownTime--;
                if (countdownTime <= 0) {
                    gameOver = true;
                    countdownTimer.stop(); // หยุดเมื่อเวลาหมด
                }
            }
        });
        countdownTimer.start();
    }

    void placePipes() {
        int randomPipeY = (int) (pipeY - pipeHeight / 4 - Math.random() * (pipeHeight / 2));
        int openingSpace = boardHeight / 4;

        // สร้างท่อบน และสุ่มว่าท่อบนนี้สามารถขยับขึ้นลงได้หรือไม่
        Pipe topPipe = new Pipe(topPipeImg, random.nextBoolean());
        topPipe.y = randomPipeY;
        pipes.add(topPipe);

        // สร้างท่อล่าง และสุ่มว่าท่อล่างนี้สามารถขยับขึ้นลงได้หรือไม่
        Pipe bottomPipe = new Pipe(bottomPipeImg, random.nextBoolean());
        bottomPipe.y = topPipe.y + pipeHeight + openingSpace;
        pipes.add(bottomPipe);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        // พื้นหลัง
        g.drawImage(backgroundImg, 0, 0, this.boardWidth, this.boardHeight, null);

        // นก
        g.drawImage(bird.img, bird.x, bird.y, bird.width, bird.height, null);

        // ท่อ
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
        }

        // เอฟเฟกต์แสงไฟกระพริบเมื่อทำคอมโบได้
        if (showComboEffect) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("Combo!", boardWidth / 4, boardHeight / 2);
        }

        // เอฟเฟกต์ระเบิด
        if (showExplosion) {
            g.drawImage(explosionImg, explosionX, explosionY, 64, 64, null); // แสดงรูปภาพระเบิด
        }

        // แสดงคะแนน
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 25));
        if (gameOver) {
            g.drawString("Game Over: " + String.valueOf((int) score), 10, 35);
        } else {
            g.drawString(String.valueOf((int) score), 10, 35);
        }

        // แสดงเวลานับถอยหลัง
        g.setFont(new Font("Arial", Font.PLAIN, 23));
        g.drawString("Time: " + countdownTime, 10, 70);

        // แสดงคะแนนสูงสุด
        g.drawString("High Score: " + String.valueOf((int) highScore), 10, 110);

        // แสดงจำนวน Combo
        g.drawString("Combo: " + comboCount + "x", 10, 150); // แสดงข้อมูลคอมโบ

        if (gameOver) {
            g.drawString("Time's up!", 10, 190);
        }
    }

    public void move() {
        // เปลี่ยนพื้นหลังตามคะแนนและเล่นเสียงเปลี่ยนพื้นหลัง
        if (score >= 50 && score < 100 && !backgroundChangedAt50) {
            backgroundImg = backgroundImg2;
            countdownTime = 60; // รีเซ็ตเวลานับถอยหลังเป็น 60
            backgroundChangedAt50 = true; // ทำเครื่องหมายว่าเปลี่ยนแล้ว
            playBackgroundChangeSound();
        } else if (score >= 100 && score < 150 && !backgroundChangedAt100) {
            backgroundImg = backgroundImg3;
            countdownTime = 60; // รีเซ็ตเวลานับถอยหลังเป็น 60
            backgroundChangedAt100 = true; // ทำเครื่องหมายว่าเปลี่ยนแล้ว
            playBackgroundChangeSound();
        } else if (score >= 150 && !backgroundChangedAt150) {
            backgroundImg = backgroundImg4;
            countdownTime = 60; // รีเซ็ตเวลานับถอยหลังเป็น 60
            backgroundChangedAt150 = true; // ทำเครื่องหมายว่าเปลี่ยนแล้ว
            playBackgroundChangeSound();
        } else if (score >= 200 && !backgroundChangedAt200) {
            backgroundImg = backgroundImg5;
            countdownTime = 60; // รีเซ็ตเวลานับถอยหลังเป็น 60
            backgroundChangedAt200 = true; // ทำเครื่องหมายว่าเปลี่ยนแล้ว
            playBackgroundChangeSound();
        }

        // การเคลื่อนที่ของนกและท่อ
        velocityY += gravity;
        bird.y += velocityY;
        bird.y = Math.max(bird.y, 0);

        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.x += pipeSpeed; // ใช้ pipeSpeed ตามระดับความยาก

            // ถ้าท่อสามารถเคลื่อนที่ขึ้นลงได้ ให้เรียกฟังก์ชัน moveVertically()
            pipe.moveVertically();

            // ถ้านกผ่านท่อแล้วให้เพิ่มคะแนน
            if (!pipe.passed && bird.x > pipe.x + pipe.width) {
                comboCount++; // นับจำนวนท่อที่ผ่านติดต่อกัน
                score += 0.5 * comboMultiplier; // เพิ่มคะแนนตามคอมโบ
                pipe.passed = true;

                // คอมโบจะเพิ่มตัวคูณคะแนนเมื่อผ่านท่อ 3, 5, 7 อันติดต่อกัน
                if (comboCount == 3 || comboCount == 5 || comboCount == 7) {
                    comboMultiplier++;
                    showComboEffect = true; // เปิดเอฟเฟกต์คอมโบ
                }
            }

            // ตรวจสอบการชนกับท่อ
            if (collision(bird, pipe)) {
                gameOver = true;
                comboCount = 0; // รีเซ็ตคอมโบเมื่อชนกับท่อ
                comboMultiplier = 1; // รีเซ็ตตัวคูณคะแนน

                // แสดงเอฟเฟกต์ระเบิด
                showExplosion = true;
                explosionX = bird.x - 30;
                explosionY = bird.y - 30;
                backgroundMusic.stop(); // หยุดเพลงเมื่อเกมจบ
            }
        }

        // ตรวจสอบว่านกหล่นออกจากขอบล่างของจอหรือไม่
        if (bird.y > boardHeight) {
            gameOver = true;
            comboCount = 0; // รีเซ็ตคอมโบเมื่อเกมจบ
            comboMultiplier = 1; // รีเซ็ตตัวคูณคะแนนเมื่อเกมจบ
            backgroundMusic.stop(); // หยุดเพลงเมื่อเกมจบ
        }

        // อัปเดตคะแนนสูงสุดเมื่อเกมจบ
        if (gameOver && score > highScore) {
            highScore = score;
        }
    }

    // ฟังก์ชันเล่นเสียงเมื่อเปลี่ยนพื้นหลัง
    public void playBackgroundChangeSound() {
        if (backgroundChangeSound != null) {
            backgroundChangeSound.setFramePosition(0); // เล่นเสียงจากจุดเริ่มต้น
            backgroundChangeSound.start();
        }
    }

    boolean collision(Bird a, Pipe b) {
        return a.x < b.x + b.width &&
               a.x + a.width > b.x &&
               a.y < b.y + b.height &&
               a.y + a.height > b.y;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) {
            placePipeTimer.stop();
            gameLoop.stop();
            countdownTimer.stop();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            velocityY = jumpStrength; // กระโดดตามระดับความยาก

            // เล่นเสียงกระโดด
            if (jumpSound != null) {
                jumpSound.setFramePosition(0); // เล่นเสียงจากจุดเริ่มต้น
                jumpSound.start();
            }
        }

        if (gameOver) {
            // รีเซ็ตเกมเมื่อกด spacebar หลังจากจบเกม
            bird.y = birdY;
            velocityY = 0;
            pipes.clear();
            gameOver = false;
            score = 0;
            countdownTime = 60; // รีเซ็ตเวลานับถอยหลัง

            // รีเซ็ต background เป็นรูปแรก
            backgroundImg = backgroundImg1;

            // รีเซ็ตตัวแปรบูลีนเพื่อติดตามการเปลี่ยนพื้นหลัง
            backgroundChangedAt50 = false;
            backgroundChangedAt100 = false;
            backgroundChangedAt150 = false;
            backgroundChangedAt200 = false;

            // รีเซ็ตเอฟเฟกต์พิเศษ
            showExplosion = false;
            showComboEffect = false;

            backgroundMusic.setFramePosition(0); // รีเซ็ตเพลงให้เริ่มจากจุดเริ่มต้น
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY); // เล่นเพลงซ้ำ

            gameLoop.start();
            placePipeTimer.start();
            countdownTimer.start();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
}