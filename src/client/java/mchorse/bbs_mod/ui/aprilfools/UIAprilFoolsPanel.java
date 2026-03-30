package mchorse.bbs_mod.ui.aprilfools;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.audio.SoundPlayer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class UIAprilFoolsPanel extends UIDashboardPanel
{
    private static final Link[] TEXTURE_55 = new Link[] {
        Link.bbs("assets/textures/55.png"),
        Link.bbs("textures/55.png"),
        Link.assets("textures/55.png"),
        Link.assets("assets/textures/55.png")
    };
    private static final Link[] TEXTURE_ROCKET = new Link[] {
        Link.bbs("assets/textures/rocket.png"),
        Link.bbs("textures/rocket.png"),
        Link.assets("textures/rocket.png"),
        Link.assets("assets/textures/rocket.png")
    };
    private static final Link[] MUSIC_BATTLE = new Link[] {
        Link.bbs("assets/audio/Mad-Mew-Mew-_UNDERTALE-Soundtrack_-Toby-Fox.ogg"),
        Link.assets("audio/Mad-Mew-Mew-_UNDERTALE-Soundtrack_-Toby-Fox.ogg")
    };
    private static final Link[] SFX_WARNING = new Link[] {
        Link.bbs("assets/audio/Warning.ogg"),
        Link.assets("audio/Warning.ogg")
    };
    private static final Link[] SFX_BONE_STAB = new Link[] {
        Link.bbs("assets/audio/BoneStab.ogg"),
        Link.assets("audio/BoneStab.ogg")
    };
    private static final Link[] SFX_PLAYER_DAMAGED = new Link[] {
        Link.bbs("assets/audio/PlayerDamaged.ogg"),
        Link.assets("audio/PlayerDamaged.ogg")
    };
    private static final Link[] SFX_DING = new Link[] {
        Link.bbs("assets/audio/Ding.ogg"),
        Link.assets("audio/Ding.ogg")
    };
    private static final Link[] SFX_MENU_CURSOR = new Link[] {
        Link.bbs("assets/audio/MenuCursor.ogg"),
        Link.assets("audio/MenuCursor.ogg")
    };
    private static final Link[] SFX_MENU_SELECT = new Link[] {
        Link.bbs("assets/audio/MenuSelect.ogg"),
        Link.assets("audio/MenuSelect.ogg")
    };
    private static final Link[] SFX_SANS_SPEAK = new Link[] {
        Link.bbs("assets/audio/SansSpeak.ogg"),
        Link.assets("audio/SansSpeak.ogg")
    };
    private static final Link[] SFX_SLAM = new Link[] {
        Link.bbs("assets/audio/Slam.ogg"),
        Link.assets("audio/Slam.ogg")
    };
    private static final Link[] SFX_GASTER_BLAST = new Link[] {
        Link.bbs("assets/audio/GasterBlast.ogg"),
        Link.assets("audio/GasterBlast.ogg")
    };
    private static final Link[] SFX_GASTER_BLAST_2 = new Link[] {
        Link.bbs("assets/audio/GasterBlast2.ogg"),
        Link.assets("audio/GasterBlast2.ogg")
    };
    private static final Link[] SFX_HEART_SHATTER = new Link[] {
        Link.bbs("assets/audio/HeartShatter.ogg"),
        Link.assets("audio/HeartShatter.ogg")
    };
    private static final Link[] SFX_HEART_SPLIT = new Link[] {
        Link.bbs("assets/audio/HeartSplit.ogg"),
        Link.assets("audio/HeartSplit.ogg")
    };
    private static final Link[] SFX_FLASH = new Link[] {
        Link.bbs("assets/audio/Flash.ogg"),
        Link.assets("audio/Flash.ogg")
    };
    private static final String[] BOSS_DIALOGUES = new String[] {
        "ready?",
        "heh... 55 time.",
        "you gonna dodge this?",
        "OHHHHHHHH",
        "stay determined... 55",
        "too easy?",
        "don't blink.",
        "bad time 55."
    };

    private static final int PHASE_MENU = 0;
    private static final int PHASE_ATTACK = 1;
    private static final int PHASE_ENEMY = 2;
    private static final int PHASE_END = 3;
    private static final int BULLET_WHITE = 0;
    private static final int BULLET_BLUE = 1;
    private static final int BULLET_ORANGE = 2;
    private static final int BULLET_WARN = 3;
    private static final int BULLET_BEAM = 4;
    private static final int BULLET_BOSS = 5;
    private static final int MODE_COUNT = 50;

    private final Random random = new Random();
    private final List<Bullet> bullets = new ArrayList<>();
    private final Map<Link, Long> sfxLastPlayed = new HashMap<>();
    private final UIButton resetButton;
    private int hp = 165;
    private int maxHp = 165;
    private int enemyHp = 555;
    private int maxEnemyHp = 555;
    private int phase = PHASE_MENU;
    private int phaseTicks;
    private int actionIndex;
    private int inputCooldown;
    private int invulTicks;
    private int waveIndex;
    private int chainRemaining;
    private int chainMinTicks;
    private int chainMaxTicks;
    private int comboCounter;
    private int warningSfxCooldown;
    private int hitSfxCooldown;
    private int battleBarkCooldown;
    private int menuSfxCooldown;
    private int bossDashCooldown;
    private int bossDashTicks;
    private boolean bossDashReturning;
    private boolean bossRoundActive;
    private int endAnimTicks;
    private boolean matchStarted;
    private boolean gameOver;
    private boolean victory;
    private boolean movedThisTick;
    private float attackCursor;
    private boolean attackForward = true;
    private String dialogue = "El Hombre de 55 apareció...";

    private float arenaX;
    private float arenaY;
    private float arenaW;
    private float arenaH;
    private float targetArenaX;
    private float targetArenaY;
    private float targetArenaW;
    private float targetArenaH;
    private float heartX;
    private float heartY;
    private float heartSize = 16;
    private float bossX;
    private float bossY;
    private float bossSize = 88;
    private float bossVx;
    private float bossVy;
    private float bossHomeX;
    private float bossHomeY;
    private float heartDeathX;
    private float heartDeathY;
    private float bossDeathX;
    private float bossDeathY;
    private long lastAnySfxMs;
    private SoundPlayer battleMusic;

    public UIAprilFoolsPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.resetButton = new UIButton(IKey.raw("Reset"), (b) -> this.resetGame());
        this.resetButton.relative(this).x(1F, -82).y(12).w(72).h(18);

        this.add(this.resetButton);
        this.resetGame();
    }

    private void resetGame()
    {
        this.bullets.clear();
        this.enemyHp = this.maxEnemyHp;
        this.hp = this.maxHp;
        this.phase = PHASE_MENU;
        this.phaseTicks = 0;
        this.actionIndex = 0;
        this.attackCursor = 0.1F;
        this.attackForward = true;
        this.inputCooldown = 0;
        this.invulTicks = 0;
        this.waveIndex = 0;
        this.chainRemaining = 0;
        this.comboCounter = 0;
        this.chainMinTicks = 0;
        this.chainMaxTicks = 0;
        this.warningSfxCooldown = 0;
        this.hitSfxCooldown = 0;
        this.battleBarkCooldown = 0;
        this.menuSfxCooldown = 0;
        this.bossDashCooldown = 0;
        this.bossDashTicks = 0;
        this.bossDashReturning = false;
        this.bossRoundActive = false;
        this.endAnimTicks = 0;
        this.bossVx = 0;
        this.bossVy = 0;
        this.bossHomeX = 0;
        this.bossHomeY = 0;
        this.heartDeathX = 0;
        this.heartDeathY = 0;
        this.bossDeathX = 0;
        this.bossDeathY = 0;
        this.lastAnySfxMs = 0;
        this.sfxLastPlayed.clear();
        this.matchStarted = false;
        this.gameOver = false;
        this.victory = false;
        this.dialogue = "El Hombre de 55 apareció... ready?";
        this.updateArena();
        this.arenaX = this.targetArenaX;
        this.arenaY = this.targetArenaY;
        this.arenaW = this.targetArenaW;
        this.arenaH = this.targetArenaH;
        this.heartX = this.arenaX + this.arenaW * 0.5F - this.heartSize * 0.5F;
        this.heartY = this.arenaY + this.arenaH - this.heartSize - 8;
        this.bossX = this.area.mx() - this.bossSize * 0.5F;
        this.bossY = this.area.y + 62;
        this.stopBattleMusic();
    }

    @Override
    public void appear()
    {}

    @Override
    public void open()
    {}

    @Override
    public void disappear()
    {
        this.stopBattleMusic();
    }

    @Override
    public void close()
    {
        this.stopBattleMusic();
    }

    @Override
    public void update()
    {
        super.update();
        BBSModClient.getSounds().update();

        if (!UIAprilFoolsOverlay.isAprilFoolsEnabled())
        {
            this.stopBattleMusic();
            return;
        }
        if (this.matchStarted)
        {
            this.ensureBattleMusic();
        }
        else
        {
            this.stopBattleMusic();
        }

        this.updateArena();
        this.arenaX += (this.targetArenaX - this.arenaX) * 0.22F;
        this.arenaY += (this.targetArenaY - this.arenaY) * 0.22F;
        this.arenaW += (this.targetArenaW - this.arenaW) * 0.22F;
        this.arenaH += (this.targetArenaH - this.arenaH) * 0.22F;

        if (Math.abs(this.arenaX - this.targetArenaX) > 200 || Math.abs(this.arenaW - this.targetArenaW) > 260)
        {
            this.arenaX = this.targetArenaX;
            this.arenaY = this.targetArenaY;
            this.arenaW = this.targetArenaW;
            this.arenaH = this.targetArenaH;
        }

        if (this.inputCooldown > 0)
        {
            this.inputCooldown--;
        }
        if (this.warningSfxCooldown > 0)
        {
            this.warningSfxCooldown--;
        }
        if (this.hitSfxCooldown > 0)
        {
            this.hitSfxCooldown--;
        }
        if (this.menuSfxCooldown > 0)
        {
            this.menuSfxCooldown--;
        }
        if (this.battleBarkCooldown > 0)
        {
            this.battleBarkCooldown--;
        }
        if (this.bossDashCooldown > 0)
        {
            this.bossDashCooldown--;
        }
        if (this.invulTicks > 0)
        {
            this.invulTicks--;
        }

        if (this.phase == PHASE_END)
        {
            this.endAnimTicks++;

            if (this.isConfirmPressed() && this.endAnimTicks > 35)
            {
                this.resetGame();
            }

            return;
        }

        if (this.phase == PHASE_MENU)
        {
            this.heartX = this.arenaX + this.arenaW * 0.5F - this.heartSize * 0.5F;
            this.heartY = this.arenaY + this.arenaH - this.heartSize - 8;
            this.bossX = this.area.mx() - this.bossSize * 0.5F;
            this.bossY = this.area.y + 62 + (float) Math.sin(System.currentTimeMillis() * 0.004D) * 2F;
            this.handleMenuInput();

            return;
        }
        if (this.phase == PHASE_ATTACK)
        {
            this.phaseTicks++;

            float speed = 0.02F;

            this.attackCursor += this.attackForward ? speed : -speed;

            if (this.attackCursor > 1F)
            {
                this.attackCursor = 1F;
                this.attackForward = false;
            }
            else if (this.attackCursor < 0F)
            {
                this.attackCursor = 0F;
                this.attackForward = true;
            }

            if (this.isConfirmPressed() || this.phaseTicks > 120)
            {
                this.resolveAttack();
            }

            return;
        }

        this.phaseTicks++;

        if (this.bossDashTicks > 0)
        {
            this.bossX += this.bossVx;
            this.bossY += this.bossVy;
            this.bossDashTicks--;

            if (!this.bossDashReturning && this.invulTicks <= 0 && this.intersects(this.bossX + 10, this.bossY + 10, this.bossSize - 20, this.bossSize - 20, this.heartX, this.heartY, this.heartSize, this.heartSize))
            {
                this.hp -= 55;
                this.invulTicks = 16;
                this.playSfx(SFX_PLAYER_DAMAGED, 1F, this.hitSfxCooldown == 0);
                this.hitSfxCooldown = 8;
            }

            if (this.bossDashTicks <= 0)
            {
                if (!this.bossDashReturning)
                {
                    this.bossDashReturning = true;
                    float dx = this.bossHomeX - this.bossX;
                    float dy = this.bossHomeY - this.bossY;
                    float len = (float) Math.sqrt(dx * dx + dy * dy);
                    float speed = 6.1F;

                    this.bossVx = dx / Math.max(0.001F, len) * speed;
                    this.bossVy = dy / Math.max(0.001F, len) * speed;
                    this.bossDashTicks = Math.max(8, (int) (len / speed));
                }
                else
                {
                    this.bossX = this.bossHomeX;
                    this.bossY = this.bossHomeY;
                    this.bossDashReturning = false;
                    this.bossDashCooldown = 45;
                    this.playSfx(SFX_SLAM, 0.95F, true);
                }
            }
        }
        else
        {
            float bossAnchorX = this.bossRoundActive ? this.arenaX + this.arenaW * 0.5F - this.bossSize * 0.5F : this.area.mx() - this.bossSize * 0.5F;
            float bossAnchorY = this.bossRoundActive ? this.arenaY - this.bossSize * 0.42F : this.area.y + 62;
            float hpRatio = this.enemyHp / (float) this.maxEnemyHp;

            this.bossX = bossAnchorX + (float) Math.sin(this.phaseTicks * 0.08F) * 6F;
            this.bossY = bossAnchorY + (float) Math.cos(this.phaseTicks * 0.13F) * 4F;

            float dashChance = hpRatio > 0.66F ? 0.16F : hpRatio > 0.33F ? 0.28F : 0.42F;

            if (this.bossRoundActive && this.bossDashCooldown <= 0 && this.phaseTicks % 72 == 0 && this.random.nextFloat() < dashChance)
            {
                this.startBossDash();
            }
        }

        if (this.phaseTicks % 8 == 0)
        {
            this.spawnPattern();
        }
        if (this.battleBarkCooldown <= 0 && this.phaseTicks % 48 == 0)
        {
            this.dialogue = "55: " + this.getBossDialogue();
            this.battleBarkCooldown = 48;
            this.playSfx(SFX_SANS_SPEAK, 1F, true);
        }

        this.handleHeartInput();

        for (int i = this.bullets.size() - 1; i >= 0; i--)
        {
            Bullet bullet = this.bullets.get(i);

            if (bullet.type == BULLET_WARN)
            {
                bullet.life--;

                if (bullet.life <= 0)
                {
                    bullet.type = BULLET_BEAM;
                    bullet.life = 20;
                    this.playSfx(SFX_BONE_STAB, 1F, this.warningSfxCooldown == 0);
                    this.warningSfxCooldown = 8;
                }

                continue;
            }
            else if (bullet.type == BULLET_BEAM)
            {
                bullet.life--;

                if (bullet.life <= 0)
                {
                    this.bullets.remove(i);
                }

                if (this.invulTicks <= 0 && this.intersectsBeam(bullet))
                {
                    this.hp -= 6;
                    this.invulTicks = 14;
                    this.playSfx(SFX_PLAYER_DAMAGED, 1F, this.hitSfxCooldown == 0);
                    this.hitSfxCooldown = 6;
                }

                continue;
            }

            bullet.x += bullet.vx;
            bullet.y += bullet.vy;

            if (bullet.x < this.arenaX - 40 || bullet.y < this.arenaY - 40 || bullet.x > this.arenaX + this.arenaW + 40 || bullet.y > this.arenaY + this.arenaH + 40)
            {
                this.bullets.remove(i);
                continue;
            }

            if (this.invulTicks <= 0 && this.intersects(bullet.x, bullet.y, bullet.size, bullet.size, this.heartX, this.heartY, this.heartSize, this.heartSize))
            {
                boolean shouldDamage = bullet.type == BULLET_WHITE
                    || bullet.type == BULLET_BOSS
                    || (bullet.type == BULLET_BLUE && this.movedThisTick)
                    || (bullet.type == BULLET_ORANGE && !this.movedThisTick);

                if (shouldDamage)
                {
                    this.hp -= 4;
                    this.invulTicks = 12;
                    this.playSfx(SFX_PLAYER_DAMAGED, 1F, this.hitSfxCooldown == 0);
                    this.hitSfxCooldown = 6;
                }
            }
        }

        if (this.hp <= 0)
        {
            this.hp = 0;
            this.triggerDefeat();

            return;
        }

        if (this.phaseTicks >= this.chainMinTicks && (this.phaseTicks >= this.chainMaxTicks || this.bullets.size() <= 4))
        {
            if (this.chainRemaining > 1)
            {
                this.chainRemaining--;
                this.comboCounter++;
                this.phaseTicks = 0;
                this.waveIndex = this.pickNextWaveIndex();
                this.bossRoundActive = this.isBossRoundMode(this.waveIndex);
                this.bullets.clear();
                this.bossDashTicks = 0;
                this.bossDashReturning = false;
                this.dialogue = "55: " + this.getBossDialogue() + " · combo x" + this.comboCounter;
                this.playSfx(SFX_DING, 0.95F + this.random.nextFloat() * 0.2F);
            }
            else
            {
                this.phase = PHASE_MENU;
                this.phaseTicks = 0;
                this.bullets.clear();
                this.dialogue = "Tu turno.";
                this.heartX = this.arenaX + this.arenaW * 0.5F - this.heartSize * 0.5F;
                this.heartY = this.arenaY + this.arenaH - this.heartSize - 8;
            }
        }
    }

    private void handleMenuInput()
    {
        if (this.inputCooldown == 0 && (Window.isKeyPressed(GLFW.GLFW_KEY_LEFT) || Window.isKeyPressed(GLFW.GLFW_KEY_A)))
        {
            this.actionIndex = (this.actionIndex + 3) % 4;
            this.inputCooldown = 8;
            if (this.menuSfxCooldown == 0)
            {
                UIUtils.playClick(0.92F);
            }
            this.menuSfxCooldown = 4;
        }
        if (this.inputCooldown == 0 && (Window.isKeyPressed(GLFW.GLFW_KEY_RIGHT) || Window.isKeyPressed(GLFW.GLFW_KEY_D)))
        {
            this.actionIndex = (this.actionIndex + 1) % 4;
            this.inputCooldown = 8;
            if (this.menuSfxCooldown == 0)
            {
                UIUtils.playClick(1.04F);
            }
            this.menuSfxCooldown = 4;
        }

        if (!this.isConfirmPressed())
        {
            return;
        }

        if (!this.matchStarted)
        {
            this.matchStarted = true;
            this.ensureBattleMusic();
        }

        UIUtils.playClick(1.2F);

        if (this.actionIndex == 0)
        {
            this.phase = PHASE_ATTACK;
            this.phaseTicks = 0;
            this.attackCursor = 0;
            this.attackForward = true;
            this.dialogue = this.getBossDialogue();
        }
        else if (this.actionIndex == 1)
        {
            this.dialogue = "ACT: " + this.getBossDialogue();
            this.playSfx(SFX_SANS_SPEAK, 1.05F);
            this.startEnemyTurn();
        }
        else if (this.actionIndex == 2)
        {
            int heal = 12;
            if (this.random.nextFloat() < 0.05F)
            {
                heal = this.maxHp - this.hp;
                this.hp = this.maxHp;
                this.dialogue = "ITEM: curación crítica FULL +" + heal + " HP.";
            }
            else
            {
                this.hp = Math.min(this.maxHp, this.hp + heal);
                this.dialogue = "ITEM: comiste un hotdog pixelado +" + heal + " HP.";
            }
            this.startEnemyTurn();
        }
        else
        {
            if (this.enemyHp <= 20)
            {
                this.triggerVictory("MERCY: perdonaste al Hombre de 55.");
            }
            else
            {
                this.dialogue = "MERCY no está disponible.";
                this.startEnemyTurn();
            }
        }
    }

    private void resolveAttack()
    {
        float dist = Math.abs(this.attackCursor - 0.5F);
        float factor = Math.max(0F, 1F - dist * 2F);
        int damage = 5 + (int) (45 * factor) + this.random.nextInt(4);

        if (this.random.nextFloat() < 0.05F)
        {
            damage = 65 + this.random.nextInt(36);
            this.dialogue = "CRÍTICO 55: ¡" + damage + " de daño!";
        }
        else
        {
            this.dialogue = "Le hiciste " + damage + " de daño.";
        }

        this.enemyHp -= damage;
        this.playSfx(SFX_BONE_STAB, 0.95F + this.random.nextFloat() * 0.12F);

        if (this.enemyHp <= 0)
        {
            this.enemyHp = 0;
            this.triggerVictory("Hombre de 55 fue derrotado.");
            return;
        }

        this.startEnemyTurn();
    }

    private void startEnemyTurn()
    {
        float hpRatio = this.enemyHp / (float) this.maxEnemyHp;
        this.phase = PHASE_ENEMY;
        this.phaseTicks = 0;
        this.waveIndex = this.pickNextWaveIndex();
        this.chainRemaining = hpRatio > 0.66F ? 2 + this.random.nextInt(2) : hpRatio > 0.33F ? 3 + this.random.nextInt(2) : 4 + this.random.nextInt(2);
        this.comboCounter = 1;
        this.chainMinTicks = hpRatio > 0.66F ? 115 : hpRatio > 0.33F ? 95 : 78;
        this.chainMaxTicks = hpRatio > 0.66F ? 190 : hpRatio > 0.33F ? 165 : 140;
        this.bullets.clear();
        this.bossDashTicks = 0;
        this.bossDashReturning = false;
        this.bossDashCooldown = 18;
        this.bossRoundActive = this.isBossRoundMode(this.waveIndex);
        this.heartX = this.arenaX + this.arenaW * 0.5F - this.heartSize * 0.5F;
        this.heartY = this.arenaY + this.arenaH - this.heartSize - 8;
        this.dialogue = "55: " + this.getBossDialogue() + " · combo de " + this.chainRemaining + " ataques.";
        this.battleBarkCooldown = 55;
        this.playSfx(SFX_SANS_SPEAK, 1F);
    }

    private int pickNextWaveIndex()
    {
        float hpRatio = this.enemyHp / (float) this.maxEnemyHp;
        int min;
        int max;

        if (hpRatio > 0.66F)
        {
            min = 0;
            max = 12;
        }
        else if (hpRatio > 0.33F)
        {
            min = 8;
            max = 28;
        }
        else
        {
            min = 14;
            max = MODE_COUNT - 1;
        }

        int[] gasterModes = new int[] {10, 13, 14, 18, 24, 31, 37, 43, 49};
        float gasterChance = hpRatio > 0.66F ? 0.08F : hpRatio > 0.33F ? 0.2F : 0.36F;

        if (this.random.nextFloat() < gasterChance)
        {
            return gasterModes[this.random.nextInt(gasterModes.length)];
        }

        return min + this.random.nextInt(max - min + 1);
    }

    private boolean isBossRoundMode(int mode)
    {
        if (mode == 10 || mode == 13 || mode == 14 || mode == 15)
        {
            return true;
        }

        if (mode < 16)
        {
            return false;
        }

        int family = Math.floorMod(mode - 16, 7);

        return family == 0 || family == 4 || family == 6;
    }

    private void spawnPattern()
    {
        int mode = this.waveIndex;
        int before = this.bullets.size();

        if (mode == 0)
        {
            Bullet bullet = new Bullet();
            bullet.type = BULLET_WHITE;
            bullet.size = 18 + this.random.nextInt(14);
            bullet.x = this.arenaX + this.random.nextFloat() * (this.arenaW - bullet.size);
            bullet.y = this.arenaY - bullet.size;
            bullet.vx = (this.random.nextFloat() * 2F - 1F) * 0.9F;
            bullet.vy = 2.9F;
            this.bullets.add(bullet);
        }
        else if (mode == 1)
        {
            Bullet bullet = new Bullet();
            bullet.type = BULLET_BLUE;
            bullet.size = 18;
            bullet.y = this.arenaY + this.random.nextFloat() * (this.arenaH - bullet.size);
            bullet.vx = 3.2F;
            bullet.vy = 0;
            bullet.x = this.arenaX - bullet.size;
            this.bullets.add(bullet);
            if (this.random.nextBoolean())
            {
                Bullet bullet2 = new Bullet();
                bullet2.type = BULLET_BLUE;
                bullet2.size = 18;
                bullet2.y = this.arenaY + this.random.nextFloat() * (this.arenaH - bullet2.size);
                bullet2.vx = -3.2F;
                bullet2.vy = 0;
                bullet2.x = this.arenaX + this.arenaW;
                this.bullets.add(bullet2);
            }
        }
        else if (mode == 2)
        {
            Bullet bullet = new Bullet();
            bullet.type = BULLET_ORANGE;
            bullet.size = 18;
            bullet.y = this.arenaY + this.random.nextFloat() * (this.arenaH - bullet.size);
            bullet.vx = -3.2F;
            bullet.vy = 0;
            bullet.x = this.arenaX + this.arenaW;
            this.bullets.add(bullet);

            Bullet mirror = new Bullet();
            mirror.type = BULLET_ORANGE;
            mirror.size = 18;
            mirror.y = this.arenaY + this.random.nextFloat() * (this.arenaH - mirror.size);
            mirror.vx = 3.2F;
            mirror.vy = 0;
            mirror.x = this.arenaX - mirror.size;
            this.bullets.add(mirror);
        }
        else if (mode == 3)
        {
            float centerX = this.arenaX + this.arenaW * 0.5F;
            float centerY = this.arenaY + this.arenaH * 0.5F;

            for (int i = 0; i < 8; i++)
            {
                float angle = (float) ((Math.PI * 2 / 8D) * i + (this.phaseTicks * 0.12F));
                Bullet bullet = new Bullet();
                bullet.type = i % 2 == 0 ? BULLET_WHITE : BULLET_BLUE;
                bullet.size = 12;
                bullet.x = centerX;
                bullet.y = centerY;
                bullet.vx = (float) Math.cos(angle) * 2.4F;
                bullet.vy = (float) Math.sin(angle) * 2.4F;
                this.bullets.add(bullet);
            }
        }
        else if (mode == 4)
        {
            float gapY = this.arenaY + 20 + this.random.nextFloat() * (this.arenaH - 40);
            float size = 14;

            for (float y = this.arenaY; y <= this.arenaY + this.arenaH - size; y += 16)
            {
                if (Math.abs(y - gapY) < 28)
                {
                    continue;
                }

                Bullet left = new Bullet();
                left.type = BULLET_WHITE;
                left.size = size;
                left.x = this.arenaX - size;
                left.y = y;
                left.vx = 3.6F;
                left.vy = 0;
                this.bullets.add(left);

                Bullet right = new Bullet();
                right.type = BULLET_WHITE;
                right.size = size;
                right.x = this.arenaX + this.arenaW;
                right.y = y;
                right.vx = -3.6F;
                right.vy = 0;
                this.bullets.add(right);
            }
        }
        else if (mode == 5)
        {
            this.playSfxCritical(SFX_WARNING, 1F, 70L);
            this.warningSfxCooldown = 12;
            Bullet warnTop = new Bullet();
            warnTop.type = BULLET_WARN;
            warnTop.beamVertical = false;
            warnTop.x = this.arenaX;
            warnTop.y = this.arenaY + 18 + this.random.nextFloat() * (this.arenaH - 36);
            warnTop.size = this.arenaW;
            warnTop.life = 20;
            this.bullets.add(warnTop);

            Bullet warnSide = new Bullet();
            warnSide.type = BULLET_WARN;
            warnSide.beamVertical = true;
            warnSide.x = this.arenaX + 18 + this.random.nextFloat() * (this.arenaW - 36);
            warnSide.y = this.arenaY;
            warnSide.size = this.arenaH;
            warnSide.life = 20;
            this.bullets.add(warnSide);
        }
        else if (mode == 6)
        {
            this.playSfx(SFX_BONE_STAB, 1F, true);
            float centerX = this.arenaX + this.arenaW * 0.5F;
            float centerY = this.arenaY + this.arenaH * 0.5F;
            float startAngle = this.phaseTicks * 0.25F;

            for (int i = 0; i < 6; i++)
            {
                float angle = startAngle + i * 1.0471F;
                Bullet bullet = new Bullet();
                bullet.type = i % 3 == 0 ? BULLET_ORANGE : BULLET_BLUE;
                bullet.size = 10 + this.random.nextInt(6);
                bullet.x = centerX;
                bullet.y = centerY;
                bullet.vx = (float) Math.cos(angle) * 2.0F;
                bullet.vy = (float) Math.sin(angle) * 2.0F;
                this.bullets.add(bullet);
            }
        }
        else if (mode == 7)
        {
            float laneX = this.arenaX + 16 + this.random.nextFloat() * (this.arenaW - 32);

            for (float y = this.arenaY - 10; y <= this.arenaY + this.arenaH + 10; y += 20)
            {
                Bullet bullet = new Bullet();
                bullet.type = ((int) ((y - this.arenaY) / 20F)) % 2 == 0 ? BULLET_BLUE : BULLET_ORANGE;
                bullet.size = 12;
                bullet.x = laneX + (this.random.nextFloat() * 18F - 9F);
                bullet.y = y;
                bullet.vx = this.random.nextFloat() * 0.8F - 0.4F;
                bullet.vy = 0.7F + this.random.nextFloat() * 0.8F;
                this.bullets.add(bullet);
            }
        }
        else if (mode == 8)
        {
            float y = this.arenaY + 16 + this.random.nextFloat() * (this.arenaH - 32);

            for (int i = 0; i < 3; i++)
            {
                Bullet warn = new Bullet();
                warn.type = BULLET_WARN;
                warn.beamVertical = false;
                warn.x = this.arenaX;
                warn.y = y + (i - 1) * 18F;
                warn.size = this.arenaW;
                warn.life = 12 + i * 3;
                this.bullets.add(warn);
            }
            this.playSfxCritical(SFX_WARNING, 1.05F, 70L);
            this.warningSfxCooldown = 10;
        }
        else if (mode == 9)
        {
            float side = this.random.nextBoolean() ? -1F : 1F;
            float sx = side < 0 ? this.arenaX - 18 : this.arenaX + this.arenaW + 18;
            float sy = this.arenaY + this.random.nextFloat() * this.arenaH;

            for (int i = 0; i < 7; i++)
            {
                Bullet bullet = new Bullet();
                bullet.type = i % 2 == 0 ? BULLET_WHITE : BULLET_BLUE;
                bullet.size = 9 + this.random.nextInt(8);
                bullet.x = sx;
                bullet.y = sy;
                float t = i / 6F;
                bullet.vx = -side * (2.2F + t * 1.6F);
                bullet.vy = (t - 0.5F) * 2.2F;
                this.bullets.add(bullet);
            }
            this.playSfx(SFX_DING, 1.1F, true);
        }
        else if (mode == 10)
        {
            this.playSfxCritical(SFX_GASTER_BLAST, 1F, 90L);
            this.playSfxCritical(SFX_GASTER_BLAST_2, 1.04F, 90L);
            this.warningSfxCooldown = 12;

            for (int i = 0; i < 2; i++)
            {
                Bullet warn = new Bullet();
                warn.type = BULLET_WARN;
                warn.beamVertical = this.random.nextBoolean();
                warn.life = 11 + i * 3;

                if (warn.beamVertical)
                {
                    warn.x = this.arenaX + 20 + this.random.nextFloat() * (this.arenaW - 40);
                    warn.y = this.arenaY;
                    warn.size = this.arenaH;
                }
                else
                {
                    warn.x = this.arenaX;
                    warn.y = this.arenaY + 20 + this.random.nextFloat() * (this.arenaH - 40);
                    warn.size = this.arenaW;
                }

                this.bullets.add(warn);
            }

            for (int i = 0; i < 3; i++)
            {
                float tx = this.arenaX + 14 + this.random.nextFloat() * (this.arenaW - 28);
                float ty = this.arenaY + 14 + this.random.nextFloat() * (this.arenaH - 28);
                float dx = tx - (this.bossX + this.bossSize * 0.5F);
                float dy = ty - (this.bossY + this.bossSize * 0.5F);
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                float nx = dx / Math.max(0.001F, len);
                float ny = dy / Math.max(0.001F, len);

                if (Math.abs(nx) < 0.0001F && Math.abs(ny) < 0.0001F)
                {
                    nx = 0;
                    ny = 1;
                }

                Bullet bullet = new Bullet();
                bullet.type = BULLET_BOSS;
                bullet.size = 26;
                bullet.x = this.bossX + this.bossSize * 0.5F - 13;
                bullet.y = this.bossY + this.bossSize * 0.5F - 13;
                bullet.vx = nx * 3.8F;
                bullet.vy = ny * 3.8F;
                this.bullets.add(bullet);
            }
        }
        else if (mode == 11)
        {
            this.playSfx(SFX_SLAM, 1F);
            float col = this.arenaX + 10 + this.random.nextFloat() * (this.arenaW - 20);

            for (float y = this.arenaY - 12; y < this.arenaY + this.arenaH + 12; y += 14)
            {
                Bullet bullet = new Bullet();
                bullet.type = BULLET_WHITE;
                bullet.size = 12;
                bullet.x = col + (this.random.nextFloat() * 8F - 4F);
                bullet.y = y;
                bullet.vx = this.random.nextFloat() * 0.4F - 0.2F;
                bullet.vy = 1.7F;
                this.bullets.add(bullet);
            }
        }
        else if (mode == 12)
        {
            float cy = this.arenaY + this.arenaH * 0.5F;

            for (int i = 0; i < 11; i++)
            {
                Bullet bullet = new Bullet();
                bullet.type = i % 2 == 0 ? BULLET_BLUE : BULLET_ORANGE;
                bullet.size = 10;
                bullet.x = this.arenaX - 12;
                bullet.y = cy + (i - 5) * 12F;
                bullet.vx = 2.8F + i * 0.12F;
                bullet.vy = (i % 2 == 0 ? 1F : -1F) * 0.45F;
                this.bullets.add(bullet);
            }
            this.playSfx(SFX_DING, 0.95F, true);
        }
        else if (mode == 13)
        {
            this.playSfxCritical(SFX_GASTER_BLAST_2, 1.08F, 90L);
            this.warningSfxCooldown = 15;

            for (int i = 0; i < 5; i++)
            {
                float t = i / 4F;
                Bullet bullet = new Bullet();
                bullet.type = BULLET_BOSS;
                bullet.size = 20;
                bullet.x = this.bossX + this.bossSize * 0.5F - bullet.size * 0.5F;
                bullet.y = this.bossY + this.bossSize * 0.5F - bullet.size * 0.5F;
                bullet.vx = (t - 0.5F) * 4.6F;
                bullet.vy = 4.2F;
                this.bullets.add(bullet);
            }

            for (int i = 0; i < 3; i++)
            {
                Bullet top = new Bullet();
                top.type = BULLET_BOSS;
                top.size = 16;
                top.x = this.arenaX + 10 + this.random.nextFloat() * (this.arenaW - 20);
                top.y = this.arenaY - 16;
                top.vx = (this.random.nextFloat() - 0.5F) * 0.8F;
                top.vy = 4.6F;
                this.bullets.add(top);
            }
        }

        if (mode == 14)
        {
            this.playSfxCritical(SFX_GASTER_BLAST, 1.1F, 90L);
            this.playSfxCritical(SFX_FLASH, 0.95F, 80L);
            float bx = this.bossX + this.bossSize * 0.5F;
            float by = this.bossY + this.bossSize * 0.5F;

            for (int i = -2; i <= 2; i++)
            {
                Bullet bullet = new Bullet();
                bullet.type = BULLET_BOSS;
                bullet.size = 16;
                bullet.x = bx - 8;
                bullet.y = by - 8;
                bullet.vx = i * 0.75F;
                bullet.vy = 4.2F;
                this.bullets.add(bullet);
            }
        }
        else if (mode == 15)
        {
            this.playSfxCritical(SFX_WARNING, 1.12F, 80L);
            float bx = this.bossX + this.bossSize * 0.5F;
            float by = this.bossY + this.bossSize * 0.5F;

            Bullet warnV = new Bullet();
            warnV.type = BULLET_WARN;
            warnV.beamVertical = true;
            warnV.x = bx;
            warnV.y = this.arenaY;
            warnV.size = this.arenaH;
            warnV.life = 14;
            this.bullets.add(warnV);

            Bullet warnH = new Bullet();
            warnH.type = BULLET_WARN;
            warnH.beamVertical = false;
            warnH.x = this.arenaX;
            warnH.y = by;
            warnH.size = this.arenaW;
            warnH.life = 14;
            this.bullets.add(warnH);

            for (int i = 0; i < 6; i++)
            {
                float angle = (float) (i * 1.04719F);
                Bullet bullet = new Bullet();
                bullet.type = BULLET_BOSS;
                bullet.size = 14;
                bullet.x = bx - 7;
                bullet.y = by - 7;
                bullet.vx = (float) Math.cos(angle) * 2.6F;
                bullet.vy = (float) Math.sin(angle) * 2.6F;
                this.bullets.add(bullet);
            }
        }
        else
        {
            this.spawnExtendedPattern(mode);
        }

        if (this.bullets.size() == before)
        {
            Bullet bullet = new Bullet();
            bullet.type = BULLET_WHITE;
            bullet.size = 16;
            bullet.x = this.arenaX + this.random.nextFloat() * (this.arenaW - 16);
            bullet.y = this.arenaY - 16;
            bullet.vx = this.random.nextFloat() * 1.2F - 0.6F;
            bullet.vy = 3.2F;
            this.bullets.add(bullet);
            this.playSfx(SFX_DING, 1F, true);
        }
    }

    private void spawnExtendedPattern(int mode)
    {
        int variant = mode - 16;
        int family = Math.floorMod(variant, 7);
        int tier = Math.floorDiv(variant, 7);
        float speedMul = 1F + tier * 0.14F;
        float sizeMul = 1F - Math.min(0.35F, tier * 0.05F);

        if (family == 0)
        {
            this.playSfxCritical(SFX_GASTER_BLAST_2, 1F + tier * 0.03F, 80L);
            float bx = this.bossX + this.bossSize * 0.5F;
            float by = this.bossY + this.bossSize * 0.5F;

            for (int i = 0; i < 6 + tier; i++)
            {
                float angle = (float) (i * (Math.PI * 2D / (6F + tier)));
                Bullet bullet = new Bullet();
                bullet.type = BULLET_BOSS;
                bullet.size = 16 * sizeMul;
                bullet.x = bx - bullet.size * 0.5F;
                bullet.y = by - bullet.size * 0.5F;
                bullet.vx = (float) Math.cos(angle) * (2.2F * speedMul);
                bullet.vy = (float) Math.sin(angle) * (2.2F * speedMul);
                this.bullets.add(bullet);
            }

            if (tier >= 2)
            {
                Bullet warn = new Bullet();
                warn.type = BULLET_WARN;
                warn.beamVertical = this.random.nextBoolean();
                warn.life = 12;
                warn.x = warn.beamVertical ? this.arenaX + 18 + this.random.nextFloat() * (this.arenaW - 36) : this.arenaX;
                warn.y = warn.beamVertical ? this.arenaY : this.arenaY + 18 + this.random.nextFloat() * (this.arenaH - 36);
                warn.size = warn.beamVertical ? this.arenaH : this.arenaW;
                this.bullets.add(warn);
            }
        }
        else if (family == 1)
        {
            this.playSfxCritical(SFX_WARNING, 1F + tier * 0.02F, 70L);
            int lanes = 2 + (tier % 4);

            for (int i = 0; i < lanes; i++)
            {
                Bullet warn = new Bullet();
                warn.type = BULLET_WARN;
                warn.beamVertical = this.random.nextBoolean();
                warn.life = 10 + tier * 2 + i * 2;

                if (warn.beamVertical)
                {
                    warn.x = this.arenaX + 12 + this.random.nextFloat() * (this.arenaW - 24);
                    warn.y = this.arenaY;
                    warn.size = this.arenaH;
                }
                else
                {
                    warn.x = this.arenaX;
                    warn.y = this.arenaY + 12 + this.random.nextFloat() * (this.arenaH - 24);
                    warn.size = this.arenaW;
                }

                this.bullets.add(warn);
            }
        }
        else if (family == 2)
        {
            this.playSfx(SFX_DING, 1F + tier * 0.02F, true);
            float cx = this.arenaX + this.arenaW * 0.5F;

            for (int i = 0; i < 8 + tier; i++)
            {
                Bullet bullet = new Bullet();
                bullet.type = i % 2 == 0 ? BULLET_BLUE : BULLET_ORANGE;
                bullet.size = 12 * sizeMul;
                bullet.x = cx + (this.random.nextFloat() - 0.5F) * 36F;
                bullet.y = this.arenaY - 16;
                bullet.vx = (this.random.nextFloat() - 0.5F) * 1.8F;
                bullet.vy = (2.8F + i * 0.16F) * speedMul;
                this.bullets.add(bullet);
            }
        }
        else if (family == 3)
        {
            this.playSfx(SFX_SLAM, 1F + tier * 0.03F, true);
            float side = this.random.nextBoolean() ? -1F : 1F;
            float startX = side < 0 ? this.arenaX - 20 : this.arenaX + this.arenaW + 20;
            float startY = this.arenaY + this.random.nextFloat() * this.arenaH;

            for (int i = 0; i < 5 + tier; i++)
            {
                float t = i / (float) Math.max(1, 4 + tier);
                Bullet bullet = new Bullet();
                bullet.type = BULLET_WHITE;
                bullet.size = 10 * sizeMul;
                bullet.x = startX;
                bullet.y = startY + (t - 0.5F) * 70F;
                bullet.vx = -side * (2.4F + t * 2.8F) * speedMul;
                bullet.vy = (t - 0.5F) * 1.6F;
                this.bullets.add(bullet);
            }
        }
        else if (family == 4)
        {
            this.playSfxCritical(SFX_GASTER_BLAST, 1.04F + tier * 0.02F, 80L);
            float bx = this.bossX + this.bossSize * 0.5F;
            float by = this.bossY + this.bossSize * 0.5F;
            int spokes = 4 + tier;

            for (int i = 0; i < spokes; i++)
            {
                float a = (float) (Math.PI * 2D * i / spokes + this.phaseTicks * 0.03F);
                Bullet bullet = new Bullet();
                bullet.type = BULLET_BOSS;
                bullet.size = 18 * sizeMul;
                bullet.x = bx - bullet.size * 0.5F;
                bullet.y = by - bullet.size * 0.5F;
                bullet.vx = (float) Math.cos(a) * (2.6F * speedMul);
                bullet.vy = (float) Math.sin(a) * (2.6F * speedMul);
                this.bullets.add(bullet);
            }
        }
        else if (family == 5)
        {
            this.playSfxCritical(SFX_FLASH, 1F + tier * 0.03F, 70L);
            float gap = this.arenaY + 18 + this.random.nextFloat() * (this.arenaH - 36);

            for (float y = this.arenaY; y <= this.arenaY + this.arenaH - 12; y += 14)
            {
                if (Math.abs(y - gap) < 24 + tier * 2)
                {
                    continue;
                }

                Bullet left = new Bullet();
                left.type = BULLET_BOSS;
                left.size = 11 * sizeMul;
                left.x = this.arenaX - left.size;
                left.y = y;
                left.vx = 3.2F * speedMul;
                left.vy = 0;
                this.bullets.add(left);

                Bullet right = new Bullet();
                right.type = BULLET_BOSS;
                right.size = 11 * sizeMul;
                right.x = this.arenaX + this.arenaW;
                right.y = y;
                right.vx = -3.2F * speedMul;
                right.vy = 0;
                this.bullets.add(right);
            }
        }
        else
        {
            this.playSfx(SFX_SANS_SPEAK, 0.96F + tier * 0.02F, true);
            float bx = this.bossX + this.bossSize * 0.5F;
            float by = this.bossY + this.bossSize * 0.5F;

            for (int i = 0; i < 3 + tier; i++)
            {
                Bullet bullet = new Bullet();
                bullet.type = BULLET_BOSS;
                bullet.size = 20 * sizeMul;
                bullet.x = bx - bullet.size * 0.5F;
                bullet.y = by - bullet.size * 0.5F;
                float tx = this.arenaX + this.random.nextFloat() * this.arenaW;
                float ty = this.arenaY + this.random.nextFloat() * this.arenaH;
                float dx = tx - bx;
                float dy = ty - by;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                bullet.vx = dx / Math.max(0.001F, len) * (3.5F * speedMul);
                bullet.vy = dy / Math.max(0.001F, len) * (3.5F * speedMul);
                this.bullets.add(bullet);
            }
        }
    }

    private void updateArena()
    {
        float panelW = this.area.w;
        float panelH = this.area.h;

        if (this.phase == PHASE_ENEMY)
        {
            this.targetArenaW = Math.min(360, Math.max(220, panelW * 0.42F));
            this.targetArenaH = Math.min(220, Math.max(140, panelH * 0.34F));
        }
        else
        {
            this.targetArenaW = Math.min(260, Math.max(180, panelW * 0.28F));
            this.targetArenaH = Math.min(150, Math.max(110, panelH * 0.22F));
        }

        this.targetArenaX = this.area.mx() - this.targetArenaW * 0.5F;
        this.targetArenaY = this.area.my() - this.targetArenaH * 0.55F;
    }

    private void startBossDash()
    {
        float hx = this.heartX + this.heartSize * 0.5F;
        float hy = this.heartY + this.heartSize * 0.5F;
        float bx = this.bossX + this.bossSize * 0.5F;
        float by = this.bossY + this.bossSize * 0.5F;
        float dx = hx - bx;
        float dy = hy - by;
        float len = (float) Math.sqrt(dx * dx + dy * dy);

        this.bossHomeX = this.bossX;
        this.bossHomeY = this.bossY;
        this.bossDashReturning = false;
        this.bossVx = dx / Math.max(0.001F, len) * 6.1F;
        this.bossVy = dy / Math.max(0.001F, len) * 6.1F;
        this.bossDashTicks = 10 + this.random.nextInt(8);
        this.dialogue = "55: directo a ti.";
        this.playSfx(SFX_SLAM, 1.12F, true);
        this.playSfxCritical(SFX_FLASH, 1F, 65L);

        for (int i = 0; i < 4; i++)
        {
            Bullet bullet = new Bullet();
            bullet.type = BULLET_BOSS;
            bullet.size = 18;
            bullet.x = bx - 9;
            bullet.y = by - 9;
            float angle = (float) (i * 1.57079F + this.random.nextFloat() * 0.3F);
            bullet.vx = (float) Math.cos(angle) * 2.8F;
            bullet.vy = (float) Math.sin(angle) * 2.8F;
            this.bullets.add(bullet);
        }
    }

    private void triggerDefeat()
    {
        this.gameOver = true;
        this.victory = false;
        this.phase = PHASE_END;
        this.dialogue = "El Hombre De 55 Años Gano.";
        this.endAnimTicks = 0;
        this.heartDeathX = this.heartX;
        this.heartDeathY = this.heartY;
        this.bullets.clear();
        this.bossDashTicks = 0;
        this.stopBattleMusic();
        this.playSfxCritical(SFX_HEART_SHATTER, 1F, 30L);
        this.playSfxCritical(SFX_HEART_SPLIT, 1F, 30L);
    }

    private void triggerVictory(String message)
    {
        this.victory = true;
        this.gameOver = false;
        this.phase = PHASE_END;
        this.dialogue = message;
        this.endAnimTicks = 0;
        this.bossDeathX = this.bossX;
        this.bossDeathY = this.bossY;
        this.bullets.clear();
        this.bossDashTicks = 0;
        this.stopBattleMusic();
        this.playSfxCritical(SFX_GASTER_BLAST_2, 1.05F, 30L);
        this.playSfxCritical(SFX_FLASH, 1F, 30L);
    }

    @Override
    public void render(UIContext context)
    {
        if (!UIAprilFoolsOverlay.isAprilFoolsEnabled())
        {
            context.batcher.text("El evento 55 está dormido... vuelve el 1 de abril 😴", this.area.x + 12, this.area.y + 12, Colors.WHITE);
            super.render(context);

            return;
        }

        Texture texture55 = this.resolveTexture(TEXTURE_55);
        Texture rocket = this.resolveTexture(TEXTURE_ROCKET);
        int pulse = (int) (Math.abs(Math.sin((System.currentTimeMillis() % 1200L) / 1200F * Math.PI * 2F)) * 35F);

        context.batcher.box(this.area.x + 8, this.area.y + 8, this.area.ex() - 8, this.area.y + 48, Colors.A50 | 0x000000);
        context.batcher.text("BATTLE 55", this.area.x + 12, this.area.y + 12, 0xFFFFCC55 + (pulse << 8));
        context.batcher.text("HP " + this.hp + "/" + this.maxHp, this.area.x + 12, this.area.y + 24, Colors.WHITE);
        context.batcher.text("ENEMY HP " + this.enemyHp + "/" + this.maxEnemyHp, this.area.x + 110, this.area.y + 24, 0xFFFF8888);
        if (this.phase == PHASE_ENEMY)
        {
            context.batcher.text("Combo: " + this.comboCounter + " / " + (this.comboCounter + this.chainRemaining - 1), this.area.x + 280, this.area.y + 24, 0xFF99CCFF);
        }

        float enemyBarX = this.area.x + 12;
        float enemyBarY = this.area.y + 38;
        float enemyBarW = 180;
        float enemyFill = enemyBarW * (this.enemyHp / (float) this.maxEnemyHp);
        context.batcher.box(enemyBarX, enemyBarY, enemyBarX + enemyBarW, enemyBarY + 6, 0xFF332222);
        context.batcher.box(enemyBarX, enemyBarY, enemyBarX + enemyFill, enemyBarY + 6, 0xFFFF6666);

        context.batcher.box(this.arenaX - 4, this.arenaY - 4, this.arenaX + this.arenaW + 4, this.arenaY + this.arenaH + 4, 0xFFEEEEEE);
        context.batcher.box(this.arenaX - 2, this.arenaY - 2, this.arenaX + this.arenaW + 2, this.arenaY + this.arenaH + 2, 0xFF111111);
        context.batcher.box(this.arenaX, this.arenaY, this.arenaX + this.arenaW, this.arenaY + this.arenaH, 0xFF050505);

        if (this.phase == PHASE_ENEMY)
        {
            for (Bullet bullet : this.bullets)
            {
                if (bullet.type == BULLET_WARN)
                {
                    int alpha = 0x55 + (int) (Math.abs(Math.sin(this.phaseTicks * 0.4F)) * 120);

                    if (bullet.beamVertical)
                    {
                        context.batcher.box(bullet.x - 3, this.arenaY, bullet.x + 3, this.arenaY + this.arenaH, (alpha << 24) | 0xFFFF4444);
                    }
                    else
                    {
                        context.batcher.box(this.arenaX, bullet.y - 3, this.arenaX + this.arenaW, bullet.y + 3, (alpha << 24) | 0xFFFF4444);
                    }
                }
                else if (bullet.type == BULLET_BEAM)
                {
                    int beamColor = 0xAAFFFFFF;

                    if (bullet.beamVertical)
                    {
                        context.batcher.box(bullet.x - 6, this.arenaY, bullet.x + 6, this.arenaY + this.arenaH, beamColor);
                    }
                    else
                    {
                        context.batcher.box(this.arenaX, bullet.y - 6, this.arenaX + this.arenaW, bullet.y + 6, beamColor);
                    }
                }
                else if (texture55 != null)
                {
                    int color = bullet.type == BULLET_BLUE ? 0xFF55AAFF : bullet.type == BULLET_ORANGE ? 0xFFFFAA33 : bullet.type == BULLET_BOSS ? 0xFFFF6666 : Colors.WHITE;
                    context.batcher.texturedBox(texture55, color, bullet.x, bullet.y, bullet.size, bullet.size, 0, 0, texture55.width, texture55.height, texture55.width, texture55.height);
                }
            }
        }

        if (rocket != null && (this.phase == PHASE_ENEMY || this.phase == PHASE_MENU))
        {
            int color = this.invulTicks > 0 && (this.invulTicks / 2) % 2 == 0 ? 0x99FFFFFF : Colors.WHITE;
            context.batcher.texturedBox(rocket, color, this.heartX, this.heartY, this.heartSize, this.heartSize, 0, 0, rocket.width, rocket.height, rocket.width, rocket.height);
        }

        if (texture55 != null)
        {
            float iy = this.bossY;
            float ix = this.bossX;
            float introSize = this.bossSize;
            float bubbleX = ix + introSize + 14;
            float bubbleY = iy + 8;
            float bubbleW = 190;
            float bubbleH = 52;
            float minX = this.area.x + 8;
            float maxX = this.area.ex() - bubbleW - 8;
            float maxY = this.area.ey() - bubbleH - 74;

            if (bubbleX > maxX)
            {
                bubbleX = ix - bubbleW - 14;
            }

            bubbleX = Math.max(minX, Math.min(bubbleX, maxX));
            bubbleY = Math.max(this.area.y + 54, Math.min(bubbleY, maxY));

            context.batcher.texturedBox(texture55, Colors.WHITE, ix, iy, introSize, introSize, 0, 0, texture55.width, texture55.height, texture55.width, texture55.height);
            context.batcher.box(bubbleX, bubbleY, bubbleX + bubbleW, bubbleY + bubbleH, 0xFFFFFFFF);
            context.batcher.box(bubbleX + 2, bubbleY + 2, bubbleX + bubbleW - 2, bubbleY + bubbleH - 2, 0xFF0A0A0A);
            context.batcher.text(this.fitBubbleText("Hombre de 55: " + this.dialogue), bubbleX + 8, bubbleY + 10, this.matchStarted ? 0xFFFFFFFF : 0xFFFFEE88);
        }

        float boxX = this.area.x + 8;
        float boxY = this.area.ey() - 66;
        float boxW = this.area.w - 16;
        float boxH = 58;

        context.batcher.box(boxX, boxY, boxX + boxW, boxY + boxH, Colors.A50 | 0x000000);
        context.batcher.box(boxX + 2, boxY + 2, boxX + boxW - 2, boxY + boxH - 2, 0xFF0F0F0F);
        context.batcher.text(this.fitBottomText(this.dialogue), boxX + 8, boxY + 8, Colors.WHITE);

        if (this.phase == PHASE_MENU)
        {
            String[] actions = {"FIGHT", "ACT", "ITEM", "MERCY"};
            float buttonY = boxY + 25;
            float buttonW = (boxW - 24) / 4F;
            float x = boxX + 6;

            for (int i = 0; i < actions.length; i++)
            {
                int border = i == this.actionIndex ? 0xFFFFAA22 : 0xFFAA5522;
                int inside = i == this.actionIndex ? 0xFF221100 : 0xFF110A06;
                context.batcher.box(x, buttonY, x + buttonW - 4, buttonY + 22, border);
                context.batcher.box(x + 2, buttonY + 2, x + buttonW - 6, buttonY + 20, inside);
                context.batcher.text(actions[i], x + 8, buttonY + 7, i == this.actionIndex ? 0xFFFFFF66 : 0xFFFFFFFF);
                x += buttonW;
            }
        }
        else if (this.phase == PHASE_ATTACK)
        {
            float barX = boxX + 10;
            float barY = boxY + 30;
            float barW = boxW - 20;
            float markerX = barX + barW * this.attackCursor;
            float centerX = barX + barW * 0.5F;

            context.batcher.box(barX, barY, barX + barW, barY + 8, 0xFF333333);
            context.batcher.box(centerX - 8, barY, centerX + 8, barY + 8, 0xFFFF4444);
            context.batcher.box(markerX - 2, barY - 2, markerX + 2, barY + 10, 0xFFFFFF55);
        }
        else if (this.phase == PHASE_END)
        {
            if (this.gameOver && rocket != null)
            {
                float t = Math.min(1F, this.endAnimTicks / 28F);
                float cx = this.heartDeathX + this.heartSize * 0.5F;
                float cy = this.heartDeathY + this.heartSize * 0.5F;
                float mainSize = this.heartSize * (1F + t * 2.2F);
                int alpha = Math.max(0, 255 - (int) (t * 255F));
                int color = (alpha << 24) | 0xFFFFFF;

                if (alpha > 0)
                {
                    context.batcher.texturedBox(rocket, color, cx - mainSize * 0.5F, cy - mainSize * 0.5F, mainSize, mainSize, 0, 0, rocket.width, rocket.height, rocket.width, rocket.height);
                }

                for (int i = 0; i < 4; i++)
                {
                    float angle = (float) (i * 1.57079F + this.endAnimTicks * 0.15F);
                    float dist = 18F + t * 56F;
                    float px = cx + (float) Math.cos(angle) * dist - 5F;
                    float py = cy + (float) Math.sin(angle) * dist - 5F;
                    int shardColor = ((Math.max(0, alpha - 40)) << 24) | 0xFF7788;
                    context.batcher.texturedBox(rocket, shardColor, px, py, 10, 10, 0, 0, rocket.width, rocket.height, rocket.width, rocket.height);
                }
            }
            else if (this.victory && texture55 != null)
            {
                float t = Math.min(1F, this.endAnimTicks / 30F);
                float cx = this.bossDeathX + this.bossSize * 0.5F;
                float cy = this.bossDeathY + this.bossSize * 0.5F;

                for (int i = 0; i < 10; i++)
                {
                    float angle = (float) (i * (Math.PI * 2D / 10D));
                    float dist = 8F + t * 96F;
                    float px = cx + (float) Math.cos(angle) * dist - 10F;
                    float py = cy + (float) Math.sin(angle) * dist - 10F;
                    int alpha = Math.max(20, 255 - (int) (t * 220F));
                    int color = (alpha << 24) | 0xFF6666;
                    context.batcher.texturedBox(texture55, color, px, py, 20, 20, 0, 0, texture55.width, texture55.height, texture55.width, texture55.height);
                }
            }

            int overlayAlpha = Math.min(220, this.endAnimTicks * 6);
            int overlayColor = this.victory ? 0x00223300 : 0x00110000;
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), (overlayAlpha << 24) | overlayColor);

            if (this.gameOver)
            {
                context.batcher.text("El Hombre De 55 Años Gano", this.area.mx() - 92, this.area.my() - 6, 0xFFFF7777);
                context.batcher.text("Reintentar (ENTER)", this.area.mx() - 54, this.area.my() + 8, 0xFFFFFFFF);
            }
            else if (this.victory)
            {
                context.batcher.text("¡GANASTE! El hombre de 55 explotó", this.area.mx() - 108, this.area.my() - 6, 0xFF66FF66);
                context.batcher.text("Continuar (ENTER)", this.area.mx() - 50, this.area.my() + 8, 0xFFFFFFFF);
            }
        }

        super.render(context);
    }

    private void handleHeartInput()
    {
        float speed = 5F;
        float beforeX = this.heartX;
        float beforeY = this.heartY;

        if (Window.isKeyPressed(GLFW.GLFW_KEY_LEFT) || Window.isKeyPressed(GLFW.GLFW_KEY_A))
        {
            this.heartX -= speed;
        }
        if (Window.isKeyPressed(GLFW.GLFW_KEY_RIGHT) || Window.isKeyPressed(GLFW.GLFW_KEY_D))
        {
            this.heartX += speed;
        }
        if (Window.isKeyPressed(GLFW.GLFW_KEY_UP) || Window.isKeyPressed(GLFW.GLFW_KEY_W))
        {
            this.heartY -= speed;
        }
        if (Window.isKeyPressed(GLFW.GLFW_KEY_DOWN) || Window.isKeyPressed(GLFW.GLFW_KEY_S))
        {
            this.heartY += speed;
        }

        this.heartX = Math.max(this.arenaX, Math.min(this.heartX, this.arenaX + this.arenaW - this.heartSize));
        this.heartY = Math.max(this.arenaY, Math.min(this.heartY, this.arenaY + this.arenaH - this.heartSize));
        this.movedThisTick = Math.abs(this.heartX - beforeX) > 0.001F || Math.abs(this.heartY - beforeY) > 0.001F;
    }

    private boolean intersects(float ax, float ay, float aw, float ah, float bx, float by, float bw, float bh)
    {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private boolean intersectsBeam(Bullet bullet)
    {
        if (bullet.beamVertical)
        {
            return this.intersects(bullet.x - 6, this.arenaY, 12, this.arenaH, this.heartX, this.heartY, this.heartSize, this.heartSize);
        }

        return this.intersects(this.arenaX, bullet.y - 6, this.arenaW, 12, this.heartX, this.heartY, this.heartSize, this.heartSize);
    }

    private boolean isConfirmPressed()
    {
        if (this.inputCooldown > 0)
        {
            return false;
        }

        if (Window.isKeyPressed(GLFW.GLFW_KEY_ENTER) || Window.isKeyPressed(GLFW.GLFW_KEY_Z) || Window.isKeyPressed(GLFW.GLFW_KEY_SPACE))
        {
            this.inputCooldown = 8;

            return true;
        }

        return false;
    }

    private void ensureBattleMusic()
    {
        if (this.battleMusic != null && this.battleMusic.isPlaying())
        {
            return;
        }

        Link link = this.resolveAudioLink(MUSIC_BATTLE);

        if (link == null)
        {
            return;
        }

        this.battleMusic = BBSModClient.getSounds().playUnique(link);

        if (this.battleMusic != null)
        {
            this.battleMusic.setRelative(true);
            this.battleMusic.setLooping(true);
            this.battleMusic.setVolume(0.38F);

            if (!this.battleMusic.isPlaying())
            {
                this.battleMusic.play();
            }
        }
    }

    private void stopBattleMusic()
    {
        if (this.battleMusic != null)
        {
            Link id = this.battleMusic.getBuffer().getId();
            this.battleMusic.stop();
            BBSModClient.getSounds().stop(id);
            this.battleMusic = null;
        }
    }

    private void playSfx(Link[] candidates, float pitch)
    {
        this.playSfx(candidates, pitch, true);
    }

    private void playSfxCritical(Link[] candidates, float pitch, long minGapMs)
    {
        BBSModClient.getSounds().update();
        Link link = this.resolveAudioLink(candidates);

        if (link == null)
        {
            return;
        }

        if (BBSModClient.getSounds().getPlayers().size() > 48)
        {
            return;
        }

        long now = System.currentTimeMillis();
        long lastByLink = this.sfxLastPlayed.getOrDefault(link, 0L);

        if (now - lastByLink < minGapMs)
        {
            return;
        }

        SoundPlayer player = BBSModClient.getSounds().play(link);

        if (player != null)
        {
            player.setRelative(true);
            player.setVolume(0.92F);
            player.setPitch(pitch);
            this.lastAnySfxMs = now;
            this.sfxLastPlayed.put(link, now);
        }
    }

    private void playSfx(Link[] candidates, float pitch, boolean allow)
    {
        if (!allow)
        {
            return;
        }

        BBSModClient.getSounds().update();

        Link link = this.resolveAudioLink(candidates);

        if (link == null)
        {
            return;
        }

        if (BBSModClient.getSounds().getPlayers().size() > 48)
        {
            return;
        }

        long now = System.currentTimeMillis();
        long lastByLink = this.sfxLastPlayed.getOrDefault(link, 0L);

        if (now - this.lastAnySfxMs < 12 || now - lastByLink < 30)
        {
            return;
        }

        SoundPlayer player = BBSModClient.getSounds().play(link);

        if (player != null)
        {
            player.setRelative(true);
            player.setVolume(0.8F);
            player.setPitch(pitch);
            this.lastAnySfxMs = now;
            this.sfxLastPlayed.put(link, now);
        }
    }

    private String fitBubbleText(String text)
    {
        return this.fitText(text, 24);
    }

    private String fitBottomText(String text)
    {
        return this.fitText(text, 74);
    }

    private String fitText(String text, int maxChars)
    {
        if (text == null || text.isEmpty() || text.length() <= maxChars)
        {
            return text;
        }

        return text.substring(0, Math.max(1, maxChars - 3)) + "...";
    }

    private String getBossDialogue()
    {
        return BOSS_DIALOGUES[this.random.nextInt(BOSS_DIALOGUES.length)];
    }

    private Link resolveAudioLink(Link[] candidates)
    {
        for (Link candidate : candidates)
        {
            if (BBSModClient.getSounds().get(candidate, false) != null)
            {
                return candidate;
            }
        }

        return null;
    }

    private static class Bullet
    {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private float size;
        private int type;
        private int life;
        private boolean beamVertical;
    }

    private Texture resolveTexture(Link[] candidates)
    {
        Texture error = BBSModClient.getTextures().getError();

        for (Link candidate : candidates)
        {
            Texture texture = BBSModClient.getTextures().getTexture(candidate);

            if (texture != null && texture != error)
            {
                return texture;
            }
        }

        return null;
    }
}
