package mchorse.bbs_mod.ui.aprilfools;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.audio.SoundPlayer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.utils.UIUtils;
import net.minecraft.client.util.math.MatrixStack;
import mchorse.bbs_mod.utils.colors.Colors;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private static final Link[][] FLOWEY_IDLE = new Link[][] {
        {
            Link.bbs("assets/textures/flowey/idle_0.png"),
            Link.assets("textures/flowey/idle_0.png")
        },
        {
            Link.bbs("assets/textures/flowey/idle_1.png"),
            Link.assets("textures/flowey/idle_1.png")
        }
    };
    private static final Link[] TEXTURE_ROCKET = new Link[] {
        Link.bbs("assets/textures/rocket.png"),
        Link.bbs("textures/rocket.png"),
        Link.assets("textures/rocket.png"),
        Link.assets("assets/textures/rocket.png")
    };
    private static final Link[] TEXTURE_HEART = new Link[] {
        Link.bbs("assets/textures/heart.png"),
        Link.assets("textures/heart.png")
    };
    private static final Link[] TEXTURE_BULLET = new Link[] {
        Link.bbs("assets/textures/bullet.png"),
        Link.bbs("textures/bullet.png"),
        Link.assets("textures/bullet.png"),
        Link.assets("assets/textures/bullet.png")
    };
    private static final Link[][] SANS_IDLE = new Link[][] {
        {
            Link.bbs("assets/textures/sans/idle_0.png"),
            Link.assets("textures/sans/idle_0.png")
        },
        {
            Link.bbs("assets/textures/sans/idle_1.png"),
            Link.assets("textures/sans/idle_1.png")
        }
    };
    private static final Link[] MUSIC_SANS = new Link[] {
        Link.bbs("assets/audio/SansBattle.ogg"),
        Link.assets("audio/SansBattle.ogg")
    };
    private static final Link[] SFX_FLOWEY_SPEAK = new Link[] {
        Link.bbs("assets/audio/FloweySpeak.wav"),
        Link.assets("audio/FloweySpeak.wav"),
        Link.bbs("assets/audio/FloweySpeak.ogg"),
        Link.assets("audio/FloweySpeak.ogg")
    };
    private static final String[] SANS_JOKES = new String[] {
        "bbs.ui.aprilfools.sans.0",
        "bbs.ui.aprilfools.sans.1",
        "bbs.ui.aprilfools.sans.2",
        "bbs.ui.aprilfools.sans.3",
        "bbs.ui.aprilfools.sans.4"
    };
    private static final Link[] MUSIC_BATTLE_EN = new Link[] {
        Link.bbs("assets/audio/FloweyBattle.ogg"),
        Link.assets("audio/FloweyBattle.ogg")
    };
    private static final Link[] MUSIC_BATTLE_ES = new Link[] {
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
        Link.bbs("assets/audio/SansVoice.ogg"),
        Link.assets("audio/SansVoice.ogg"),
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
        "bbs.ui.aprilfools.dialogue.0",
        "bbs.ui.aprilfools.dialogue.1",
        "bbs.ui.aprilfools.dialogue.2",
        "bbs.ui.aprilfools.dialogue.3",
        "bbs.ui.aprilfools.dialogue.4",
        "bbs.ui.aprilfools.dialogue.5",
        "bbs.ui.aprilfools.dialogue.6",
        "bbs.ui.aprilfools.dialogue.7"
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
    private static final int MODE_COUNT = 26;
    private static final int[] SANS_TIMELINE = new int[] {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
        10, 11, 12, 13, 14, 16, 17, 18,
        19, 20, 21, 22, 23, 24, 25
    };

    private final Random random = new Random();
    private final List<Bullet> bullets = new ArrayList<>();
    private int floweyIdleFrame = 0;
    private long floweyIdleLastFrame = 0L;
    private final Map<Link, Long> sfxLastPlayed = new HashMap<>();
    private final UIButton resetButton;
    private int hp = 20;
    private int maxHp = 20;
    private int enemyHp = 200;
    private int maxEnemyHp = 200;
    private int phase = PHASE_MENU;
    private int phaseTicks;
    private int actionIndex;
    private int inputCooldown;
    private int invulTicks;
    private int waveIndex;
    private int attackStep;
    private int attackPhaseIndex = -1;
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
    private int playerAttackAnimTicks;
    private int playerAttackDamage;
    private boolean bossDodgedHit;
    private int endAnimTicks;
    private boolean matchStarted;
    private boolean gameOver;
    private boolean victory;
    private boolean movedThisTick;
    private float attackCursor;
    private boolean attackForward = true;
    private String dialogue = "";
    private boolean sansPhase = false;
    private long battleStartMs = 0L;
    private long flashbangMs = 0L;
    private int sansIdleFrame = 0;
    private long sansIdleLastFrame = 0L;
    private int sansIdleTicks = 0;
    private int fightFlashTicks = 0;
    private boolean fightWasCrit = false;
    private int actVfxTicks = 0;
    private int healVfxTicks = 0;
    private int healVfxAmount = 0;
    private int mercyVfxTicks = 0;
    private int floweyRoundsDone = 0;

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
    private float safeLaneY;
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
    private float playerAttackX;
    private float playerAttackY;
    private long lastAnySfxMs;
    private SoundPlayer battleMusic;

    public UIAprilFoolsPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.resetButton = new UIButton(L10n.lang("bbs.ui.aprilfools.reset"), (b) -> this.resetGame());
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
        this.attackStep = 0;
        this.attackPhaseIndex = -1;
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
        this.playerAttackAnimTicks = 0;
        this.playerAttackDamage = 0;
        this.bossDodgedHit = false;
        this.endAnimTicks = 0;
        this.bossVx = 0;
        this.bossVy = 0;
        this.bossHomeX = 0;
        this.bossHomeY = 0;
        this.heartDeathX = 0;
        this.heartDeathY = 0;
        this.bossDeathX = 0;
        this.bossDeathY = 0;
        this.playerAttackX = 0;
        this.playerAttackY = 0;
        this.lastAnySfxMs = 0;
        this.sfxLastPlayed.clear();
        this.matchStarted = false;
        this.gameOver = false;
        this.sansPhase = false;
        this.battleStartMs = 0L;
        this.flashbangMs = 0L;
        this.sansIdleFrame = 0;
        this.sansIdleLastFrame = 0L;
        this.sansIdleTicks = 0;
        this.fightFlashTicks = 0;
        this.fightWasCrit = false;
        this.actVfxTicks = 0;
        this.healVfxTicks = 0;
        this.healVfxAmount = 0;
        this.mercyVfxTicks = 0;
        this.floweyRoundsDone = 0;
        this.victory = false;
        this.dialogue = this.tr("bbs.ui.aprilfools.dialogue.intro");
        this.updateArena();
        this.arenaX = this.targetArenaX;
        this.arenaY = this.targetArenaY;
        this.arenaW = this.targetArenaW;
        this.arenaH = this.targetArenaH;
        this.heartX = this.arenaX + this.arenaW * 0.5F - this.heartSize * 0.5F;
        this.heartY = this.arenaY + this.arenaH - this.heartSize - 8;
        this.safeLaneY = this.arenaY + this.arenaH * 0.5F;
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
            if (this.flashbangMs > 0L && !this.sansPhase
                    && System.currentTimeMillis() - this.flashbangMs >= 400L)
            {
                this.sansPhase = true;
                this.stopBattleMusic();
                this.dialogue = this.tr("bbs.ui.aprilfools.sans.intro");
                this.enemyHp = this.maxEnemyHp;
            }

            if (this.sansPhase)
            {
                this.ensureSansMusic();
            }
            else
            {
                this.ensureBattleMusic();
            }
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
        if (this.playerAttackAnimTicks > 0)
        {
            this.playerAttackAnimTicks--;
        }
        if (this.bossDashCooldown > 0)
        {
            this.bossDashCooldown--;
        }
        if (this.invulTicks > 0)
        {
            this.invulTicks--;
        }
        if (this.fightFlashTicks > 0)
        {
            this.fightFlashTicks--;
        }
        if (this.actVfxTicks > 0)
        {
            this.actVfxTicks--;
        }
        if (this.healVfxTicks > 0)
        {
            this.healVfxTicks--;
        }
        if (this.mercyVfxTicks > 0)
        {
            this.mercyVfxTicks--;
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

            if (this.sansPhase && this.matchStarted)
            {
                this.sansIdleTicks++;

                if (this.sansIdleTicks >= 100)
                {
                    this.sansIdleTicks = 0;
                    this.dialogue = this.getSansJoke();
                    this.playSfx(SFX_SANS_SPEAK, 1F, true);
                }
            }

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

        int spawnInterval = this.enemyHp > this.maxEnemyHp * 0.66F ? 12 : this.enemyHp > this.maxEnemyHp * 0.33F ? 10 : 9;

        if (this.phaseTicks % spawnInterval == 0)
        {
            this.spawnPattern();
        }
        if (this.battleBarkCooldown <= 0 && this.phaseTicks % 48 == 0)
        {
            this.dialogue = this.sansPhase
                ? this.getSansJoke()
                : this.tf("bbs.ui.aprilfools.dialogue.speak", this.getBossDialogue());
            this.battleBarkCooldown = 48;
            this.playSfx(this.sansPhase || !UIAprilFoolsOverlay.isEnglish() ? SFX_SANS_SPEAK : SFX_FLOWEY_SPEAK, 1F, true);
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
                this.waveIndex = this.nextOrderedMode();
                this.bossRoundActive = this.isBossRoundMode(this.waveIndex);
                this.bullets.clear();
                this.bossDashTicks = 0;
                this.bossDashReturning = false;
                this.dialogue = this.tf("bbs.ui.aprilfools.dialogue.combo_x", this.getBossDialogue(), this.comboCounter);
                this.playSfx(SFX_DING, 0.95F + this.random.nextFloat() * 0.2F);
            }
            else
            {
                this.phase = PHASE_MENU;
                this.phaseTicks = 0;
                this.bullets.clear();
                this.dialogue = this.tr("bbs.ui.aprilfools.dialogue.your_turn");
                this.heartX = this.arenaX + this.arenaW * 0.5F - this.heartSize * 0.5F;
                this.heartY = this.arenaY + this.arenaH - this.heartSize - 8;

                /* After first Flowey round, trigger flashbang → sans */
                if (!this.sansPhase && this.flashbangMs == 0L)
                {
                    this.floweyRoundsDone++;

                    if (this.floweyRoundsDone >= 1)
                    {
                        this.flashbangMs = System.currentTimeMillis();
                    }
                }
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

        this.sansIdleTicks = 0;

        if (this.actionIndex == 0)
        {
            this.phase = PHASE_ATTACK;
            this.phaseTicks = 0;
            this.attackCursor = 0;
            this.attackForward = true;
            this.dialogue = this.sansPhase ? this.tr("bbs.ui.aprilfools.sans.fight") : this.getBossDialogue();
        }
        else if (this.actionIndex == 1)
        {
            this.dialogue = this.sansPhase
                ? this.tr("bbs.ui.aprilfools.sans.act")
                : this.tf("bbs.ui.aprilfools.dialogue.act", this.getBossDialogue());
            this.playSfx(SFX_SANS_SPEAK, 1.05F);
            this.startEnemyTurn();
            this.actVfxTicks = 18;
        }
        else if (this.actionIndex == 2)
        {
            int heal = 12;
            if (this.random.nextFloat() < 0.05F)
            {
                heal = this.maxHp - this.hp;
                this.hp = this.maxHp;
                this.dialogue = this.tf("bbs.ui.aprilfools.dialogue.item_full", heal);
            }
            else
            {
                this.hp = Math.min(this.maxHp, this.hp + heal);
                this.dialogue = this.tf("bbs.ui.aprilfools.dialogue.item_heal", heal);
            }
            this.startEnemyTurn();
            this.healVfxTicks = 20;
            this.healVfxAmount = heal;
        }
        else
        {
            if (this.enemyHp <= 20)
            {
                this.triggerVictory(this.tr("bbs.ui.aprilfools.dialogue.mercy_win"));
            }
            else
            {
                this.dialogue = this.sansPhase
                    ? this.tr("bbs.ui.aprilfools.sans.mercy")
                    : this.tr("bbs.ui.aprilfools.dialogue.mercy_no");
                this.startEnemyTurn();
                this.mercyVfxTicks = 16;
            }
        }
    }

    private void resolveAttack()
    {
        float dist = Math.abs(this.attackCursor - 0.5F);
        float factor = Math.max(0F, 1F - dist * 2F);
        int damage = 5 + (int) (45 * factor) + this.random.nextInt(4);
        boolean dodge = this.random.nextFloat() < 0.12F;
        boolean isCrit = this.random.nextFloat() < 0.05F;

        if (isCrit)
        {
            damage = 65 + this.random.nextInt(36);
            if (!dodge)
            {
                this.dialogue = this.tf("bbs.ui.aprilfools.dialogue.crit", damage);
            }
        }
        else if (!dodge)
        {
            this.dialogue = this.tf("bbs.ui.aprilfools.dialogue.hit", damage);
        }

        this.playerAttackAnimTicks = 18;
        this.playerAttackDamage = dodge ? 0 : damage;
        this.bossDodgedHit = dodge;
        this.playerAttackX = this.bossX + this.bossSize * 0.5F;
        this.playerAttackY = this.bossY + this.bossSize * 0.45F;

        if (dodge)
        {
            this.dialogue = this.tr("bbs.ui.aprilfools.dialogue.dodge");
            this.playSfx(SFX_DING, 1.08F, true);
        }
        else
        {
            this.enemyHp -= damage;
            this.playSfx(SFX_BONE_STAB, 0.95F + this.random.nextFloat() * 0.12F);
            this.fightFlashTicks = 14;
            this.fightWasCrit = isCrit;
        }

        if (this.enemyHp <= 0)
        {
            this.enemyHp = 0;
            this.triggerVictory(this.tr("bbs.ui.aprilfools.dialogue.boss_defeated"));
            return;
        }

        this.startEnemyTurn();
    }

    private void startEnemyTurn()
    {
        float hpRatio = this.enemyHp / (float) this.maxEnemyHp;
        this.phase = PHASE_ENEMY;
        this.phaseTicks = 0;
        this.waveIndex = this.nextOrderedMode();
        this.chainRemaining = hpRatio > 0.66F ? 2 : hpRatio > 0.33F ? 3 : 4;
        this.comboCounter = 1;
        this.chainMinTicks = hpRatio > 0.66F ? 118 : hpRatio > 0.33F ? 96 : 74;
        this.chainMaxTicks = hpRatio > 0.66F ? 188 : hpRatio > 0.33F ? 158 : 132;
        this.bullets.clear();
        this.bossDashTicks = 0;
        this.bossDashReturning = false;
        this.bossDashCooldown = 18;
        this.bossRoundActive = this.isBossRoundMode(this.waveIndex);
        this.heartX = this.arenaX + this.arenaW * 0.5F - this.heartSize * 0.5F;
        this.heartY = this.arenaY + this.arenaH - this.heartSize - 8;
        this.safeLaneY = this.arenaY + 20 + this.random.nextFloat() * (this.arenaH - 40);
        this.dialogue = this.tf("bbs.ui.aprilfools.dialogue.combo_count", this.getBossDialogue(), this.chainRemaining);
        this.battleBarkCooldown = 55;
        this.playSfx(SFX_SANS_SPEAK, 1F);
    }

    private int nextOrderedMode()
    {
        float hpRatio = this.enemyHp / (float) this.maxEnemyHp;
        int phaseIndex = hpRatio > 0.66F ? 0 : hpRatio > 0.33F ? 1 : 2;

        if (this.attackPhaseIndex != phaseIndex)
        {
            this.attackPhaseIndex = phaseIndex;
            this.attackStep = 0;
        }

        int maxIndex = phaseIndex == 0 ? 9 : phaseIndex == 1 ? 17 : SANS_TIMELINE.length - 1;
        int localStep = Math.min(this.attackStep, maxIndex);
        int mode = SANS_TIMELINE[localStep];

        if (this.attackStep < maxIndex)
        {
            this.attackStep++;
        }
        else if (phaseIndex == 2)
        {
            int[] endLoop = new int[] {19, 20, 21, 22, 23, 24, 25};
            mode = endLoop[(this.attackStep - maxIndex) % endLoop.length];
            this.attackStep++;
        }

        return mode;
    }

    private boolean isBossRoundMode(int mode)
    {
        return mode == 10 || mode == 13 || mode == 14 || mode == 15 || mode == 18 || mode == 20 || mode == 24 || mode == 25;
    }

    private void spawnPattern()
    {
        if (this.bullets.size() > 72)
        {
            return;
        }

        if (this.bullets.size() > 48 && this.phaseTicks % 2 == 0)
        {
            return;
        }

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
            float gapY = Math.max(this.arenaY + 20, Math.min(this.safeLaneY + (this.random.nextFloat() - 0.5F) * 10F, this.arenaY + this.arenaH - 20));
            float size = 14;
            boolean fromLeft = (this.attackStep + this.comboCounter) % 2 == 0;

            for (float y = this.arenaY; y <= this.arenaY + this.arenaH - size; y += 16)
            {
                if (Math.abs(y - gapY) < 36)
                {
                    continue;
                }

                Bullet bullet = new Bullet();
                bullet.type = BULLET_WHITE;
                bullet.size = size;
                bullet.x = fromLeft ? this.arenaX - size : this.arenaX + this.arenaW;
                bullet.y = y;
                bullet.vx = fromLeft ? 3.2F : -3.2F;
                bullet.vy = 0;
                this.bullets.add(bullet);
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
        else if (mode == 16)
        {
            this.playSfx(SFX_BONE_STAB, 1.02F, true);
            float gapY = this.arenaY + 18 + this.random.nextFloat() * (this.arenaH - 36);

            for (float y = this.arenaY; y <= this.arenaY + this.arenaH - 12; y += 14)
            {
                if (Math.abs(y - gapY) < 22)
                {
                    continue;
                }

                Bullet left = new Bullet();
                left.type = BULLET_WHITE;
                left.size = 12;
                left.x = this.arenaX - 12;
                left.y = y;
                left.vx = 4.3F;
                left.vy = 0;
                this.bullets.add(left);
            }
        }
        else if (mode == 17)
        {
            this.playSfx(SFX_DING, 1.02F, true);

            for (int i = 0; i < 8; i++)
            {
                Bullet bullet = new Bullet();
                bullet.type = i % 2 == 0 ? BULLET_BLUE : BULLET_ORANGE;
                bullet.size = 12;
                bullet.x = this.arenaX + this.random.nextFloat() * this.arenaW;
                bullet.y = this.arenaY - 14;
                bullet.vx = (this.random.nextFloat() - 0.5F) * 1.3F;
                bullet.vy = 4.2F + i * 0.12F;
                this.bullets.add(bullet);
            }
        }
        else if (mode == 18)
        {
            this.playSfxCritical(SFX_GASTER_BLAST_2, 1.08F, 80L);
            float bx = this.bossX + this.bossSize * 0.5F;
            float by = this.bossY + this.bossSize * 0.5F;

            for (int i = 0; i < 6; i++)
            {
                float angle = (float) (i * 1.04719F);
                Bullet bullet = new Bullet();
                bullet.type = BULLET_BOSS;
                bullet.size = 16;
                bullet.x = bx - 8;
                bullet.y = by - 8;
                bullet.vx = (float) Math.cos(angle) * 3.2F;
                bullet.vy = (float) Math.sin(angle) * 3.2F;
                this.bullets.add(bullet);
            }
        }
        else if (mode == 19)
        {
            this.playSfx(SFX_SLAM, 1.02F, true);
            float centerX = this.arenaX + this.arenaW * 0.5F;
            float safeHalfGap = 26F;

            for (int i = 0; i < 8; i++)
            {
                Bullet bullet = new Bullet();
                bullet.type = BULLET_WHITE;
                bullet.size = 10;
                float side = i % 2 == 0 ? -1F : 1F;
                bullet.x = centerX + side * (safeHalfGap + this.random.nextFloat() * 26F);
                bullet.y = this.arenaY - 10 - i * 12F;
                bullet.vx = (this.random.nextFloat() - 0.5F) * 0.8F;
                bullet.vy = 3.3F;
                this.bullets.add(bullet);
            }
        }
        else if (mode == 20)
        {
            this.playSfxCritical(SFX_GASTER_BLAST, 1F, 85L);
            float bx = this.bossX + this.bossSize * 0.5F;
            float by = this.bossY + this.bossSize * 0.5F;

            for (int i = 0; i < 4; i++)
            {
                float tx = this.arenaX + this.random.nextFloat() * this.arenaW;
                float ty = this.arenaY + this.random.nextFloat() * this.arenaH;
                float dx = tx - bx;
                float dy = ty - by;
                float len = (float) Math.sqrt(dx * dx + dy * dy);

                Bullet bullet = new Bullet();
                bullet.type = BULLET_BOSS;
                bullet.size = 18;
                bullet.x = bx - 9;
                bullet.y = by - 9;
                bullet.vx = dx / Math.max(0.001F, len) * 4.1F;
                bullet.vy = dy / Math.max(0.001F, len) * 4.1F;
                this.bullets.add(bullet);
            }
        }
        else if (mode == 21)
        {
            this.playSfxCritical(SFX_WARNING, 1.06F, 70L);
            float y = this.arenaY + 20 + this.random.nextFloat() * (this.arenaH - 40);

            for (int i = -1; i <= 1; i++)
            {
                Bullet warn = new Bullet();
                warn.type = BULLET_WARN;
                warn.beamVertical = false;
                warn.x = this.arenaX;
                warn.y = y + i * 16F;
                warn.size = this.arenaW;
                warn.life = 14;
                this.bullets.add(warn);
            }
        }
        else if (mode == 22)
        {
            this.playSfxCritical(SFX_WARNING, 1.1F, 70L);
            float x = this.arenaX + 18 + this.random.nextFloat() * (this.arenaW - 36);

            for (int i = -1; i <= 1; i++)
            {
                Bullet warn = new Bullet();
                warn.type = BULLET_WARN;
                warn.beamVertical = true;
                warn.x = x + i * 14F;
                warn.y = this.arenaY;
                warn.size = this.arenaH;
                warn.life = 13;
                this.bullets.add(warn);
            }
        }
        else if (mode == 23)
        {
            this.playSfx(SFX_BONE_STAB, 1.1F, true);
            int gapIndex = this.random.nextInt(9);
            boolean fromLeft = (this.attackStep + this.comboCounter) % 2 == 0;

            for (int i = 0; i < 9; i++)
            {
                if (Math.abs(i - gapIndex) <= 1)
                {
                    continue;
                }

                Bullet bullet = new Bullet();
                bullet.type = i % 2 == 0 ? BULLET_ORANGE : BULLET_WHITE;
                bullet.size = 12;
                bullet.x = fromLeft ? this.arenaX - 12 : this.arenaX + this.arenaW + 12;
                bullet.y = this.arenaY + 8 + i * ((this.arenaH - 16) / 9F);
                bullet.vx = fromLeft ? 3.5F : -3.5F;
                bullet.vy = (this.random.nextFloat() - 0.5F) * 0.5F;
                this.bullets.add(bullet);
            }
        }
        else if (mode == 24)
        {
            this.playSfxCritical(SFX_GASTER_BLAST_2, 1.14F, 80L);
            float bx = this.bossX + this.bossSize * 0.5F;
            float by = this.bossY + this.bossSize * 0.5F;

            for (int i = 0; i < 6; i++)
            {
                float angle = (float) (i * (Math.PI * 2D / 6D) + this.phaseTicks * 0.06F);
                Bullet bullet = new Bullet();
                bullet.type = BULLET_BOSS;
                bullet.size = 15;
                bullet.x = bx - 7.5F;
                bullet.y = by - 7.5F;
                bullet.vx = (float) Math.cos(angle) * 3.2F;
                bullet.vy = (float) Math.sin(angle) * 3.2F;
                this.bullets.add(bullet);
            }
        }
        else if (mode == 25)
        {
            this.playSfxCritical(SFX_GASTER_BLAST, 1.15F, 80L);
            this.playSfxCritical(SFX_WARNING, 1.15F, 70L);

            Bullet warnV = new Bullet();
            warnV.type = BULLET_WARN;
            warnV.beamVertical = true;
            warnV.x = this.arenaX + this.arenaW * 0.5F;
            warnV.y = this.arenaY;
            warnV.size = this.arenaH;
            warnV.life = 18;
            this.bullets.add(warnV);

            Bullet warnH = new Bullet();
            warnH.type = BULLET_WARN;
            warnH.beamVertical = false;
            warnH.x = this.arenaX;
            warnH.y = this.arenaY + this.arenaH * 0.5F;
            warnH.size = this.arenaW;
            warnH.life = 18;
            this.bullets.add(warnH);

            for (int i = 0; i < 4; i++)
            {
                Bullet bullet = new Bullet();
                bullet.type = BULLET_BOSS;
                bullet.size = 14;
                bullet.x = this.bossX + this.bossSize * 0.5F - 7;
                bullet.y = this.bossY + this.bossSize * 0.5F - 7;
                float t = i / 3F;
                bullet.vx = (t - 0.5F) * 3.9F;
                bullet.vy = 3.8F;
                this.bullets.add(bullet);
            }
        }
        else
        {
            this.playSfx(SFX_DING, 1F, true);
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
        this.dialogue = this.tr("bbs.ui.aprilfools.dialogue.direct");
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
        this.dialogue = this.tr("bbs.ui.aprilfools.end.defeat_title");
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
            context.batcher.text(this.tr("bbs.ui.aprilfools.sleeping"), this.area.x + 12, this.area.y + 12, Colors.WHITE);
            super.render(context);

            return;
        }

        Texture texture55 = this.resolveTexture(TEXTURE_55);
        Texture rocket = this.resolveTexture(TEXTURE_ROCKET);
        Texture heartTex = this.resolveTexture(TEXTURE_HEART);
        Texture bulletTex = this.resolveTexture(TEXTURE_BULLET);
        int pulse = (int) (Math.abs(Math.sin((System.currentTimeMillis() % 1200L) / 1200F * Math.PI * 2F)) * 35F);

        context.batcher.box(this.area.x + 8, this.area.y + 8, this.area.ex() - 8, this.area.y + 48, Colors.A50 | 0x000000);
        context.batcher.text(this.tr(this.sansPhase ? "bbs.ui.aprilfools.sans.battle_title" : "bbs.ui.aprilfools.battle_title"), this.area.x + 12, this.area.y + 12, 0xFFFFCC55 + (pulse << 8));
        context.batcher.text(this.tf("bbs.ui.aprilfools.hp", this.hp, this.maxHp), this.area.x + 12, this.area.y + 24, Colors.WHITE);
        context.batcher.text(this.tf("bbs.ui.aprilfools.enemy_hp", this.enemyHp, this.maxEnemyHp), this.area.x + 110, this.area.y + 24, 0xFFFF8888);
        if (this.phase == PHASE_ENEMY)
        {
            context.batcher.text(this.tf("bbs.ui.aprilfools.combo_label", this.comboCounter, this.comboCounter + this.chainRemaining - 1), this.area.x + 280, this.area.y + 24, 0xFF99CCFF);
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
                else
                {
                    Texture btex = UIAprilFoolsOverlay.isEnglish()
                        ? (bulletTex != null ? bulletTex : texture55)
                        : texture55;

                    if (btex != null)
                    {
                        int color = bullet.type == BULLET_BLUE ? 0xFF55AAFF : bullet.type == BULLET_ORANGE ? 0xFFFFAA33 : bullet.type == BULLET_BOSS ? 0xFFFF6666 : Colors.WHITE;
                        int step = (int) (System.currentTimeMillis() / 150L) % 4;
                        float angle = (float) Math.toRadians(step * 90);
                        float cx = bullet.x + bullet.size * 0.5F;
                        float cy = bullet.y + bullet.size * 0.5F;
                        float half = bullet.size * 0.5F;
                        MatrixStack matrices = context.batcher.getContext().getMatrices();

                        matrices.push();
                        matrices.translate(cx, cy, 0F);
                        matrices.multiply(new org.joml.Quaternionf().rotateZ(angle));
                        context.batcher.texturedBox(btex, color, -half, -half, bullet.size, bullet.size, 0, 0, btex.width, btex.height, btex.width, btex.height);
                        matrices.pop();
                    }
                }
            }
        }

        if (this.phase == PHASE_ENEMY || this.phase == PHASE_MENU)
        {
            int color = this.invulTicks > 0 && (this.invulTicks / 2) % 2 == 0 ? 0x99FFFFFF : Colors.WHITE;

            if (UIAprilFoolsOverlay.isEnglish() && heartTex != null)
            {
                context.batcher.texturedBox(heartTex, color, this.heartX, this.heartY, this.heartSize, this.heartSize, 0, 0, heartTex.width, heartTex.height, heartTex.width, heartTex.height);
            }
            else if (rocket != null)
            {
                context.batcher.texturedBox(rocket, color, this.heartX, this.heartY, this.heartSize, this.heartSize, 0, 0, rocket.width, rocket.height, rocket.width, rocket.height);
            }
        }

        if (texture55 != null)
        {
            float iy = this.bossY;
            float ix = this.bossX;
            float introSize = this.bossSize;

            long idleNow = System.currentTimeMillis();

            if (idleNow - this.floweyIdleLastFrame >= 500L)
            {
                this.floweyIdleFrame = (this.floweyIdleFrame + 1) % 2;
                this.floweyIdleLastFrame = idleNow;
            }

            if (idleNow - this.sansIdleLastFrame >= 500L)
            {
                this.sansIdleFrame = (this.sansIdleFrame + 1) % 2;
                this.sansIdleLastFrame = idleNow;
            }

            Texture floweyTex = UIAprilFoolsOverlay.isEnglish()
                ? (this.sansPhase
                    ? this.resolveTexture(SANS_IDLE[this.sansIdleFrame])
                    : this.resolveTexture(FLOWEY_IDLE[this.floweyIdleFrame]))
                : null;

            float bubbleX = ix + introSize + 14;
            float bubbleY = iy + 8;
            float bubbleW = 260;
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

            if (floweyTex != null)
            {
                int fw = floweyTex.width * 2;
                int fh = floweyTex.height * 2;
                float fcx = ix + introSize * 0.5F - fw * 0.5F;
                float fcy = iy + introSize * 0.5F - fh * 0.5F;

                context.batcher.texturedBox(floweyTex, Colors.WHITE, fcx, fcy, fw, fh, 0, 0, floweyTex.width, floweyTex.height, floweyTex.width, floweyTex.height);
            }
            else
            {
                context.batcher.texturedBox(texture55, Colors.WHITE, ix, iy, introSize, introSize, 0, 0, texture55.width, texture55.height, texture55.width, texture55.height);
            }

            if (this.playerAttackAnimTicks > 0)
            {
                float t = this.playerAttackAnimTicks / 18F;
                float swing = 26F * (1F - t);
                int slashAlpha = Math.max(40, (int) (220 * t));
                int slashColor = (slashAlpha << 24) | (this.bossDodgedHit ? 0x66CCFF : 0xFF5555);
                float sx = this.playerAttackX - 8 + swing;
                float sy = this.playerAttackY - 10;
                context.batcher.box(sx - 2, sy, sx + 18, sy + 3, slashColor);
                context.batcher.box(sx + 2, sy + 4, sx + 20, sy + 7, slashColor);

                if (this.bossDodgedHit)
                {
                    context.batcher.text(this.tr("bbs.ui.aprilfools.miss"), this.playerAttackX + 12, this.playerAttackY - 16, 0xFF99DDFF);
                }
                else
                {
                    context.batcher.text("-" + this.playerAttackDamage, this.playerAttackX + 12, this.playerAttackY - 16, 0xFFFF7777);
                }
            }

            context.batcher.box(bubbleX, bubbleY, bubbleX + bubbleW, bubbleY + bubbleH, 0xFFFFFFFF);
            context.batcher.box(bubbleX + 2, bubbleY + 2, bubbleX + bubbleW - 2, bubbleY + bubbleH - 2, 0xFF0A0A0A);
            String bubbleKey = this.sansPhase ? "bbs.ui.aprilfools.sans.bubble" : "bbs.ui.aprilfools.bubble";
            context.batcher.text(this.fitBubbleText(this.tf(bubbleKey, this.dialogue)), bubbleX + 8, bubbleY + 10, this.matchStarted ? 0xFFFFFFFF : 0xFFFFEE88);
        }

        /* VFX overlays */
        if (this.fightFlashTicks > 0)
        {
            float t = this.fightFlashTicks / 14F;
            float expand = (1F - t) * 55F;
            int alpha = (int) (t * 180) & 0xFF;
            int color = this.fightWasCrit ? 0xFF6600 : 0xFF2222;
            float bcx = this.bossX + this.bossSize * 0.5F;
            float bcy = this.bossY + this.bossSize * 0.5F;

            /* expanding impact rings */
            context.batcher.box(bcx - expand, bcy - expand * 0.5F, bcx + expand, bcy + expand * 0.5F, (alpha << 24) | color);
            context.batcher.box(bcx - expand * 0.6F, bcy - expand * 0.3F, bcx + expand * 0.6F, bcy + expand * 0.3F, ((alpha / 2) << 24) | 0xFFFFFF);

            /* cross flash lines */
            MatrixStack vfxMat = context.batcher.getContext().getMatrices();
            vfxMat.push();
            vfxMat.translate(bcx, bcy, 0F);
            vfxMat.multiply(new org.joml.Quaternionf().rotateZ((float) Math.toRadians((1F - t) * 45F)));
            float len = expand * 1.4F;
            context.batcher.box(-len, -2, len, 2, (alpha << 24) | 0xFFFFFF);
            context.batcher.box(-2, -len, 2, len, (alpha << 24) | 0xFFFFFF);
            vfxMat.pop();
        }

        if (this.actVfxTicks > 0)
        {
            float t = this.actVfxTicks / 18F;
            int alpha = (int) (t * 140) & 0xFF;
            float bcx = this.bossX + this.bossSize * 0.5F;
            float bcy = this.bossY + this.bossSize * 0.5F;
            float r = (1F - t) * 50F + 10F;

            for (int i = 0; i < 6; i++)
            {
                float angle = (float) (i * Math.PI / 3 + (1F - t) * Math.PI);
                float sx = bcx + (float) Math.cos(angle) * r;
                float sy = bcy + (float) Math.sin(angle) * r * 0.6F;
                int sz = (int) (4 + t * 4);
                context.batcher.box(sx - sz, sy - sz, sx + sz, sy + sz, (alpha << 24) | 0xFFDD44);
            }
        }

        if (this.healVfxTicks > 0)
        {
            float t = this.healVfxTicks / 20F;
            float rise = (1F - t) * 28F;
            int alpha = (int) (t * 200) & 0xFF;
            float hcx = this.heartX + this.heartSize * 0.5F;
            float hcy = this.heartY - rise;

            context.batcher.box(hcx - 20, hcy - 2, hcx + 20, hcy + 14, ((alpha / 3) << 24) | 0x00FF44);
            context.batcher.text("+" + this.healVfxAmount + " HP", hcx - 18, hcy + 2, (alpha << 24) | 0xFF66FF44);
        }

        if (this.mercyVfxTicks > 0)
        {
            float t = this.mercyVfxTicks / 16F;
            float expand = (1F - t) * 40F;
            int alpha = (int) (t * 160) & 0xFF;
            float bcx = this.bossX + this.bossSize * 0.5F;
            float bcy = this.bossY + this.bossSize * 0.5F;

            context.batcher.box(bcx - expand * 1.4F, bcy - expand, bcx + expand * 1.4F, bcy + expand, (alpha << 24) | 0xFF88CC);
            context.batcher.box(bcx - expand, bcy - expand * 0.6F, bcx + expand, bcy + expand * 0.6F, ((alpha / 2) << 24) | 0xFFAAFF);
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
            String[] actions = {
                this.tr("bbs.ui.aprilfools.action.fight"),
                this.tr("bbs.ui.aprilfools.action.act"),
                this.tr("bbs.ui.aprilfools.action.item"),
                this.tr("bbs.ui.aprilfools.action.mercy")
            };
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

        /* Flashbang white overlay for flowey→sans transition */
        if (this.flashbangMs > 0L)
        {
            long fbElapsed = System.currentTimeMillis() - this.flashbangMs;
            float fbAlpha;

            if (fbElapsed < 400L)
            {
                fbAlpha = 1F;
            }
            else
            {
                fbAlpha = Math.max(0F, 1F - (fbElapsed - 400L) / 500F);
            }

            if (fbAlpha > 0F)
            {
                int fa = (int) (fbAlpha * 255) & 0xFF;
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), (fa << 24) | 0xFFFFFF);
            }
        }

            if (this.gameOver)
            {
                String t1 = this.tr("bbs.ui.aprilfools.end.defeat_title");
                String t2 = this.tr("bbs.ui.aprilfools.end.retry");
                context.batcher.text(t1, this.centeredTextX(t1), this.area.my() - 6, 0xFFFF7777);
                context.batcher.text(t2, this.centeredTextX(t2), this.area.my() + 8, 0xFFFFFFFF);
            }
            else if (this.victory)
            {
                String t1 = this.tr("bbs.ui.aprilfools.end.victory_title");
                String t2 = this.tr("bbs.ui.aprilfools.end.continue");
                context.batcher.text(t1, this.centeredTextX(t1), this.area.my() - 6, 0xFF66FF66);
                context.batcher.text(t2, this.centeredTextX(t2), this.area.my() + 8, 0xFFFFFFFF);
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

        Link link = this.resolveAudioLink(UIAprilFoolsOverlay.isEnglish() ? MUSIC_BATTLE_EN : MUSIC_BATTLE_ES);

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

    private void ensureSansMusic()
    {
        if (this.battleMusic != null && this.battleMusic.isPlaying())
        {
            return;
        }

        Link link = this.resolveAudioLink(MUSIC_SANS);

        if (link == null)
        {
            return;
        }

        this.battleMusic = BBSModClient.getSounds().playUnique(link);

        if (this.battleMusic != null)
        {
            this.battleMusic.setRelative(true);
            this.battleMusic.setLooping(true);
            this.battleMusic.setVolume(0.45F);

            if (!this.battleMusic.isPlaying())
            {
                this.battleMusic.play();
            }
        }
    }

    private String getSansJoke()
    {
        return this.tr(SANS_JOKES[this.random.nextInt(SANS_JOKES.length)]);
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
        return this.fitText(text, 40);
    }

    private String fitBottomText(String text)
    {
        return this.fitText(text, 74);
    }

    private float centeredTextX(String text)
    {
        return this.area.mx() - (text == null ? 0 : text.length() * 3F);
    }

    private String tr(String key)
    {
        return L10n.lang(key).get();
    }

    private String tf(String key, Object... args)
    {
        return String.format(Locale.ROOT, this.tr(key), args);
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
        return this.tr(BOSS_DIALOGUES[this.random.nextInt(BOSS_DIALOGUES.length)]);
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
