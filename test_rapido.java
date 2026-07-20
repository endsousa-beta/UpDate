package end.player.tv;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.makeramen.roundedimageview.RoundedImageView;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import end.player.tv.utils.FolderItem;
import end.player.tv.utils.MediaRepository;
import end.player.tv.utils.OnItemFocusListener;
import end.player.tv.utils.PrefsHelper;
import end.player.tv.utils.ShimmerEfeito;
import end.player.tv.utils.VideoFile;

import end.player.tv.theme.AppTheme;
import end.player.tv.theme.ThemeConfig;
import end.player.tv.theme.ThemeManager;

public class MainHome extends AppCompatActivity implements OnItemFocusListener {

    // ===============================
    // CONSTANTES
    // ===============================
    public static final String SET_MAIN = "MAIN";
    private static final int UPDATE_REQUEST_CODE = 100;
    private static final long BACKGROUND_DELAY = 300L;
    private static final long BACKGROUND_FADE_IN_MS = 200L;
    private static final long BACKGROUND_FADE_OUT_MS = 300L;
    private static final int MENU_RENDER = 200;
    private static final int MENU_ANIM_TIME = 500;
    private static final float MENU_ANIM_DISTANCE = 100f;
    private static final float DEVICE_TRANSLATION_Y = 80f;

    public static int ICON, LOGO, THUMBR, IMAGE, VIDEO;
    public static int SEM_020, SEM_040, SEM_080, SEM_100;
    public static int COM_020, COM_040, COM_080, COM_100;
    public static int BLACK_080, BLACK_100;
    public static int WHITE_080, WHITE_100;

    public String THEME;

    // ===============================
    // VIEWS
    // ===============================
    private LinearLayout toolbar_down;
    private ImageView recyclerWallpaper, imgBackgroundFront, imgBackgroundBack;
    private ExoPlayer player;

    private RelativeLayout storageContainer;
    private TextView storage_pvCenter, storage_pvTop, storage_pvBottom;
    private LinearLayout storage_actvContent;

    // ===============================
    // HELPERS
    // ===============================

    // Handler Ãºnico (evita acumular callbacks)
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PrefsHelper prefsHelper;
    private ThemeManager themeManager;
    private Runnable focusRunnable;
    private Runnable renderItemsRunnable;
    private View mGradient;
    private final List<String> displayItems = new ArrayList<>();

    // keys
    private long lastBackClickTime = 0;
    private boolean isNavigating = false; // Membro da classe (global)

    //private Object currentBackground = null;

    // ===============================
    // CONTROLE
    // ===============================
    private boolean WS_NULL = false;
    private Dialog rightSheetDialog;
    private int currentIndex = 0;
    private int lastSectionIndex = -1;
    private int lastTabIndex = -1;
    private final int currentTab = 0;
    private boolean isBloqueador = false;

    private Object lastImagePath = null;

    private final List<MenuSection> sections = new ArrayList<>();

    // ===============================
    // MODELOS
    // ===============================

    public enum SectionType { DISPOSITIVOS, TEMAS }

    public static class MenuSection {

        public final SectionType type;
        public final String title;
        public final File storage;
        public final List<String> items; // ðŸ”¥ usado em TEMAS

        private String currentPath;
        public int lastFocusedPosition = 0;

        // ===============================
        // CONSTRUTOR: DISPOSITIVO
        // ===============================
        public MenuSection(String title, File storage) {
            this.type = SectionType.DISPOSITIVOS;
            this.title = title;
            this.storage = storage;
            this.items = null;

            if (storage != null) {
                this.currentPath = storage.getAbsolutePath();
            } else {
                this.currentPath = null;
            }
        }

        // ===============================
        // CONSTRUTOR: TEMAS
        // ===============================
        public MenuSection(SectionType type, String title, List<String> items) {
            this.type = type;
            this.title = title;
            this.items = items;
            this.storage = null;
            this.currentPath = null;
        }

        public String getCurrentPath() {
            return currentPath;
        }

        public void goTo(String path) {
            this.currentPath = path;
            this.lastFocusedPosition = 0;
        }

        public String getDisplayName() {
            if (type == SectionType.TEMAS) {
                return title;
            }

            if (storage != null && storage.getName() != null && !storage.getName().isEmpty()) {
                return storage.getName();
            }

            return (title != null && !title.isEmpty()) ? title : "Dispositivo";
        }
    }
    /**
     * Modelo base unificado de â€˜itemâ€™ (local ou remoto).
     */
    public static abstract class ExplorerItem {
        public final String name;
        public final String path;
        public final boolean folder;

        protected ExplorerItem(String name, String path, boolean folder) {
            this.name = name != null ? name : "";
            this.path = path != null ? path : "";
            this.folder = folder;
        }

        public boolean isFolder() {
            return folder;
        }
    }

    /**
     * Item vindo de `FolderItem` (local).
     */
    public static class LocalItem extends ExplorerItem {
        public final Uri uri;
        public final FolderItem source;

        public LocalItem(FolderItem source) {
            super(
                    source != null ? source.name : "",
                    source != null ? source.fullPath : "",
                    source != null && source.isFolder
            );
            this.source = source;
            this.uri = (source != null) ? source.uri : null;
        }
    }

    // wallpaper
    private Object lastResolvedPath = null;


    // ===============================
    // CICLO DE VIDA
    // ===============================
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // ==============================
        // 1. PERSISTÃƒÅ NCIA / CONFIGURAÃƒâ€¡ÃƒÆ’O
        // ==============================
        inicializarPreferencias();
        prefsHelper = new PrefsHelper(this);

        initInAppUpdate();

        // ==============================
        // 2. BIND DAS VIEWS
        // ==============================
        storageContainer = findViewById(R.id.explorer_root);
        storage_pvTop = findViewById(R.id.explorer_top);
        storage_pvBottom = findViewById(R.id.explorer_bottom);
        storage_pvCenter = findViewById(R.id.explorer_title);
        storage_actvContent = findViewById(R.id.explorer_content);

        imgBackgroundBack = findViewById(R.id.mainBackground);
        imgBackgroundFront = findViewById(R.id.mainWallpaper);
        mGradient = findViewById(R.id.explorer_gradient);

        // ==============================
        // 3. BACKGROUND BASE (FIXO)
        // ==============================
        if (imgBackgroundBack != null) {
            imgBackgroundBack.setImageResource(IMAGE);
            imgBackgroundBack.setAlpha(1f);

            // Estado sincronizado do sistema de fundo
            lastResolvedPath = IMAGE;
            isFrontVisible = false;
        }

        // ==============================
        // 4. LAYER FRONT (PREVIEW)
        // ==============================
        if (imgBackgroundFront != null) {
            imgBackgroundFront.setImageDrawable(null);
            imgBackgroundFront.setAlpha(0f);
        }

        // ==============================
        // 5. ESTADO INICIAL DA UI
        // ==============================
        currentIndex = 0;

        // ==============================
        // 6. INICIALIZAÃƒâ€¡ÃƒÆ’O DO FLUXO
        // ==============================
        inicializarSections();
    }

    private void inicializarSections() {

        // limpa a estrutura existente (sem recriar objeto)
        sections.clear();

        // ==============================
        // 1. DISPOSITIVOS LOCAIS
        // ==============================
        ArrayList<File> listaDeDiscos = getStorages();

        if (listaDeDiscos != null && !listaDeDiscos.isEmpty()) {
            for (File disco : listaDeDiscos) {
                if (disco == null) continue;

                sections.add(
                        new MenuSection(
                                getStorageType(disco),
                                disco
                        )
                );
            }
        }

        // ==============================
        // 2. SEÃƒâ€¡ÃƒÆ’O FIXA: TEMAS
        // ==============================
        sections.add(
                new MenuSection(
                        SectionType.TEMAS,
                        "SELEÃ‡ÃƒO DE TEMAS",
                        Arrays.asList(
                                "PADRÃƒO",
                                "ANEMO",
                                "GEO",
                                "ELECTRO",
                                "DENDRO",
                                "HYDRO",
                                "PYRO",
                                "CRYO"
                        )
                )
        );

        // ==============================
        // 3. ESTADO INICIAL
        // ==============================
        if (sections.isEmpty()) return;

        currentIndex = 0;

        // ==============================
        // 4. PRIMEIRA RENDERIZAÃƒâ€¡ÃƒÆ’O
        // ==============================
        storageContainer.post(() -> {
            if (isFinishing() || isDestroyed()) return;

            listaVerticalSubir(storageContainer, mGradient);
        });
    }

    // ===============================
    // EXPLORER INICIAL
    // ===============================

    private void listaVerticalSubir(View v, View v2) {
        if (v == null) return;

        isBloqueador = true;

        v.animate().cancel();
        v.setAlpha(0f);
        v.setTranslationY(150f);
        v.setVisibility(View.VISIBLE);

        if (v2 != null) {
            v2.animate().cancel();
            v2.setAlpha(0f);
            v2.setTranslationY(150f);
            v2.setVisibility(View.VISIBLE);
        }

        v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(MENU_ANIM_TIME)
                .setInterpolator(new OvershootInterpolator(0.8f))
                .withEndAction(() -> {
                    isBloqueador = false;

                    // leve delay evita jank de foco na animaÃƒÂ§ÃƒÂ£o
                    v.post(v::requestFocus);
                })
                .start();

        if (v2 != null) {
            v2.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(MENU_ANIM_TIME)
                    .setInterpolator(new OvershootInterpolator(0.8f))
                    .start();
        }
    }

    private void listaVerticalDescer(View v, View v2, Runnable finalizador) {
        isBloqueador = true;

        if (v == null || v.getVisibility() != View.VISIBLE) {
            if (finalizador != null) finalizador.run();
            isBloqueador = false;
            return;
        }

        // 1. Cancela animaÃ§Ãµes anteriores com seguranÃ§a
        v.animate().cancel();
        if (v2 != null) v2.animate().cancel();

        // 2. AnimaÃ§Ã£o do contÃªiner principal (v) usando a GPU (.withLayer())
        v.animate()
                .alpha(0f)
                .translationY(150f)
                .setDuration(BACKGROUND_FADE_IN_MS)
                .setInterpolator(new AccelerateInterpolator())
                .withLayer() // ðŸ”¥ CRÃTICO PARA TV: Joga o peso da renderizaÃ§Ã£o para a GPU
                .withEndAction(() -> {
                    // Roda na Thread de UI de forma limpa
                    v.setVisibility(View.GONE);
                    v.setTranslationY(0f);
                    v.setAlpha(1f); // ðŸ”¥ Reset preventivo para quando a view voltar a ser visÃ­vel

                    if (v2 != null) {
                        v2.setVisibility(View.GONE);
                        v2.setTranslationY(0f);
                        v2.setAlpha(0f);
                    }

                    if (finalizador != null) {
                        // ðŸ”¥ SEGREDO SÃŠNIOR: PostDelayed curto de 30ms dÃ¡ tempo para o Android
                        // limpar a fila de desenho antes do Player travar a memÃ³ria abrindo.
                        v.postDelayed(() -> {
                            finalizador.run();
                            isBloqueador = false;
                        }, 30L);
                    } else {
                        isBloqueador = false;
                    }
                })
                .start();

        // 3. AnimaÃ§Ã£o do gradiente (v2) em perfeita sincronia tambÃ©m usando a GPU
        if (v2 != null) {
            v2.animate()
                    .alpha(0f)
                    .translationY(150f)
                    .setDuration(BACKGROUND_FADE_IN_MS)
                    .setInterpolator(new AccelerateInterpolator())
                    .withLayer() // ðŸ”¥ Melhora o desempenho do desenho de gradientes na TV
                    .start();
        }
    }

   // ==============================
   // ONRESUME (OTIMIZADO)
   // ==============================

    private boolean primeiraEntrada = true;

    @Override
    protected void onResume() {
        super.onResume();

        inicializarPreferencias();

        // ==============================
        // 1. RESTAURAÃ‡ÃƒO DE THUMB (CORRIGIDO)
        // ==============================
        final String itemAt = prefsHelper.getLastVideoImage();

        if (imgBackgroundFront != null && imgBackgroundBack != null) {
            // Cancela animaÃ§Ãµes residuais que vieram de antes da abertura do player
            imgBackgroundFront.animate().cancel();
            imgBackgroundBack.animate().cancel();

            if (itemAt != null && !itemAt.trim().isEmpty()) {
                // ForÃ§amos o reset do rastro para que a Trava MÃ¡gica PERMITA o carregamento
                lastResolvedPath = null;
                lastImagePath = "";
                configurarMidia(itemAt);
            } else {
                lastResolvedPath = IMAGE;
                lastImagePath = "";

                // Limpeza segura usando o ciclo de vida do Glide
                Glide.with(this).clear(imgBackgroundFront);
                Glide.with(this).clear(imgBackgroundBack);

                imgBackgroundFront.setAlpha(0f);
                imgBackgroundFront.setVisibility(View.GONE);

                imgBackgroundBack.setBackgroundColor(COM_020);
                imgBackgroundBack.setImageResource(IMAGE); // Garanta o ID correto do recurso
                imgBackgroundBack.setAlpha(1f);
                imgBackgroundBack.setVisibility(View.VISIBLE);
            }
        }

        // ==============================
        // 2. VALIDAÃ‡ÃƒO DE SEÃ‡Ã•ES
        // ==============================
        if (sections == null || sections.isEmpty()) return;

        // ==============================
        // 3. PRIMEIRA ENTRADA
        // ==============================
        if (primeiraEntrada) {
            if (storageContainer != null) storageContainer.setVisibility(View.VISIBLE);
            if (mGradient != null) mGradient.setVisibility(View.VISIBLE);

            listaVertical();

            primeiraEntrada = false;
            return;
        }

        // ==============================
        // 4. LIMPEZA DE ESTADO DE FOCO ANTERIOR
        // ==============================
        // Remova apenas o runnable especÃ­fico do foco para nÃ£o matar processos internos da UI
        if (focusRunnable != null) {
            mainHandler.removeCallbacks(focusRunnable);
        }
        isBloqueador = true;

        // Marca UI como "suja" para evitar bug de cache/return
        lastRenderedPath = null;

        // ==============================
        // 5. PREPARA UI PARA RETORNO (ANIMAÃ‡ÃƒO EM PLAY)
        // ==============================
        if (storageContainer != null) {
            storageContainer.setVisibility(View.INVISIBLE);
            storageContainer.setAlpha(0f);
            storageContainer.setTranslationY(175f);
        }

        if (mGradient != null) {
            mGradient.setVisibility(View.INVISIBLE);
            mGradient.setAlpha(0.3f);
            mGradient.setTranslationY(175f);
        }

        // ==============================
        // 6. RENDER ÃšNICO E SEGURO (VIEW TREE OBSERVER BLINDADO)
        // ==============================
        if (storageContainer != null) {
            storageContainer.post(() -> {
                if (isFinishing() || isDestroyed()) return;

                // ConstrÃ³i a Ã¡rvore de carrossÃ©is
                listaVertical();

                storageContainer.getViewTreeObserver()
                        .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                            @Override
                            public boolean onPreDraw() {
                                // Desvincula imediatamente para evitar loops infinitos de desenho
                                if (storageContainer.getViewTreeObserver().isAlive()) {
                                    storageContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                                }

                                storageContainer.post(() -> {
                                    if (isFinishing() || isDestroyed()) return;

                                    storageContainer.setVisibility(View.VISIBLE);
                                    if (mGradient != null) mGradient.setVisibility(View.VISIBLE);

                                    // Janela milimÃ©trica para o Android consolidar o desenho fÃ­sico antes da subida cinÃ©tica
                                    storageContainer.postDelayed(() -> {
                                        if (!isFinishing() && !isDestroyed()) {
                                            listaVerticalSubir(storageContainer, mGradient);
                                        }
                                    }, 100L);
                                });

                                return true;
                            }
                        });
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // ==============================
        // 1. CANCELAMENTO DE TAREFAS ASSÃƒÂNCRONAS
        // ==============================
        mainHandler.removeCallbacksAndMessages(null);

        if (storage_actvContent != null) {
            storage_actvContent.removeCallbacks(renderItemsRunnable);
        }

        // ==============================
        // 2. ANIMAÃƒâ€¡Ãƒâ€¢ES DO MENU
        // ==============================
        if (storageContainer != null) {
            storageContainer.animate().cancel();

            // evita estado visual inconsistente
            if (storageContainer.getVisibility() == View.VISIBLE) {
                storageContainer.setAlpha(1f);
            }
        }

        if (mGradient != null) {
            mGradient.animate().cancel();
        }

        // ==============================
        // 3. BACKGROUND (DOUBLE BUFFERING)
        // ==============================
        if (imgBackgroundFront != null) {
            imgBackgroundFront.animate().cancel();
        }

        if (imgBackgroundBack != null) {
            imgBackgroundBack.animate().cancel();
        }

        // ==============================
        // 4. RESET DE CONTROLE
        // ==============================
        isBloqueador = false;
    }

    // ==============================
    // ONSTOP
    // ==============================
    @Override
    protected void onStop() {
        super.onStop();

        // ==============================
        // 1. STATE DE PLAYER (RESET CONTROLADO)
        // ==============================
        if (prefsHelper != null) {
            prefsHelper.setLastVideoImage(null);
        }

        // ==============================
        // 2. STATE LOCAL (MEMÃƒâ€œRIA)
        // ==============================
        lastResolvedPath = null;
        lastImagePath = null;
    }


    // ==============================
    // ONDESTROY
    // ==============================
    @Override
    protected void onDestroy() {

        // ==============================
        // 1. ZERA CALLBACKS
        // ==============================
        renderItemsRunnable = null;
        focusRunnable = null;
        lastResolvedPath = null;
        lastImagePath = null;

        // ==============================
        // 2. GLIDE CLEANUP (CORRETO)
        // ==============================
        if (imgBackgroundFront != null) {
            Glide.with(this).clear(imgBackgroundFront);
        }

        if (imgBackgroundBack != null) {
            Glide.with(this).clear(imgBackgroundBack);
        }

        // ==============================
        // 3. ANIMAÃƒâ€¡Ãƒâ€¢ES
        // ==============================
        if (imgBackgroundFront != null) imgBackgroundFront.animate().cancel();
        if (imgBackgroundBack != null) imgBackgroundBack.animate().cancel();

        // ==============================
        // 4. PREFS (IMPORTANTE AJUSTE)
        // ==============================
        // Ã¢Å¡Â Ã¯Â¸Â EVITE limpar aqui se quiser restaurar estado apÃƒÂ³s reabrir app
        // manter isso pode causar "flicker" ao reentrar app
        if (prefsHelper != null) {
            prefsHelper.setLastVideoImage(null);
        }

        super.onDestroy();
    }




    // ===============================
    // ATUALIZAÃ‡ÃƒO IN-APP
    // ===============================
    private void initInAppUpdate() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            UPDATE_REQUEST_CODE
                    );
                } catch (IntentSender.SendIntentException e) {
                    Log.e("Update", "Erro ao iniciar update", e);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                Log.d("Update", "UsuÃ¡rio cancelou a atualizaÃ§Ã£o.");
            } else if (resultCode != RESULT_OK) {
                Log.e("Update", "Falha na atualizaÃ§Ã£o. CÃ³digo: " + resultCode);
            }
        }
    }

    // ===============================
    // PREFERÃŠNCIAS / TEMA
    // ===============================
    private void inicializarPreferencias() {
        SharedPreferences prefs = getSharedPreferences(SET_MAIN, MODE_PRIVATE);

        SEM_020 = prefs.getInt("MAIN_SEM_020", ContextCompat.getColor(this, R.color.PADRAO_SEM_020));
        SEM_040 = prefs.getInt("MAIN_SEM_040", ContextCompat.getColor(this, R.color.PADRAO_SEM_040));
        SEM_080 = prefs.getInt("MAIN_SEM_080", ContextCompat.getColor(this, R.color.PADRAO_SEM_080));
        SEM_100 = prefs.getInt("MAIN_SEM_100", ContextCompat.getColor(this, R.color.PADRAO_SEM_100));

        COM_020 = prefs.getInt("MAIN_COM_020", ContextCompat.getColor(this, R.color.PADRAO_COM_020));
        COM_040 = prefs.getInt("MAIN_COM_040", ContextCompat.getColor(this, R.color.PADRAO_COM_040));
        COM_080 = prefs.getInt("MAIN_COM_080", ContextCompat.getColor(this, R.color.PADRAO_COM_080));
        COM_100 = prefs.getInt("MAIN_COM_100", ContextCompat.getColor(this, R.color.PADRAO_COM_100));

        BLACK_080 = prefs.getInt("MAIN_BLACK_080", ContextCompat.getColor(this, R.color.NIGHT_080));
        BLACK_100 = prefs.getInt("MAIN_BLACK_100", ContextCompat.getColor(this, R.color.NIGHT_100));

        WHITE_080 = prefs.getInt("MAIN_WHITE_080", ContextCompat.getColor(this, R.color.WHITE_080));
        WHITE_100 = prefs.getInt("MAIN_WHITE_100", ContextCompat.getColor(this, R.color.WHITE_100));

        ICON = prefs.getInt("MAIN_ICON", R.mipmap.padrao_icon);
        LOGO = prefs.getInt("MAIN_LOGO", R.mipmap.padrao_logo);
        THUMBR = prefs.getInt("MAIN_THUMBR", R.mipmap.padrao_thumb);
        //VIDEO = prefs.getInt("MAIN_VIDEO", R.raw.padrao_wallpaper);
        IMAGE = prefs.getInt("MAIN_IMAGE", R.mipmap.padrao_wallpaper);

        THEME = prefs.getString("MAIN_THEME", "PADRÃƒO");
    }

    // ===============================
    // STORAGE
    // ===============================
    private ArrayList<File> getStorages() {
        File[] dirs = getExternalFilesDirs(null);
        ArrayList<File> roots = new ArrayList<>();

        for (File dir : dirs) {
            if (dir == null) continue;

            File root = dir;
            for (int i = 0; i < 4; i++) {
                if (root.getParentFile() != null) {
                    root = root.getParentFile();
                }
            }

            if (root.exists() && !roots.contains(root)) {
                roots.add(root);
            }
        }

        File internal = Environment.getExternalStorageDirectory();
        if (!roots.contains(internal)) {
            roots.add(internal);
        }

        return roots;
    }

    private String getStorageType(File storage) {
        if (storage == null) return "Armazenamento";
        String path = storage.getAbsolutePath().toLowerCase();

        if (path.contains("emulated") || path.endsWith("/0")) {
            return "Interno";
        } else if (path.contains("usb")
                || path.contains("otg")
                || (path.startsWith("/storage/") && !path.contains("self"))) {
            return "Pendrive";
        } else if (path.contains("sd")) {
            return "CartÃ£o SD";
        }
        return "Armazenamento";
    }

    // ===============================
    // MENU / LISTAGEM
    // ===============================
    private final Map<String, List<FolderItem>> folderCache = new HashMap<>();
    private String lastRenderedPath = null;

    @SuppressLint("NewApi")
    public void listaVertical() {

        // 1. ValidaÃ§Ãµes Iniciais
        if (sections == null || sections.isEmpty()) return;
        if (currentIndex < 0 || currentIndex >= sections.size()) return;
        if (storageContainer == null || storage_actvContent == null) return;
        if (isFinishing() || isDestroyed()) return;

        final MenuSection current = sections.get(currentIndex);
        if (current == null) return;

        // RESET VISUAL
        storage_actvContent.animate().cancel();

        if (renderItemsRunnable != null) {
            storage_actvContent.removeCallbacks(renderItemsRunnable);
        }

        storage_actvContent.setAlpha(1f);
        storage_actvContent.setTranslationY(0);
        storage_actvContent.removeAllViews();

        // ANIMAÃ‡ÃƒO
        if (currentIndex != lastSectionIndex) {

            float dirY = (currentIndex > lastSectionIndex && lastSectionIndex != -1)
                    ? DEVICE_TRANSLATION_Y
                    : -DEVICE_TRANSLATION_Y;

            storageContainer.animate().cancel();
            storageContainer.setTranslationY(dirY);
            storageContainer.setAlpha(0f);

            storageContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(MENU_ANIM_TIME)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();

            lastSectionIndex = currentIndex;
            lastTabIndex = currentTab;

            if (imgBackgroundFront != null) {
                imgBackgroundFront.animate().cancel();
            }

        } else if (currentTab != lastTabIndex) {
            lastTabIndex = currentTab;
        }

        updatePreviews();

        // TÃTULO
        if (storage_pvCenter != null) {
            storage_pvCenter.setText(current.getDisplayName());
            storage_pvCenter.setPadding(5, 0, 0, 0);
            storage_pvCenter.setTextColor(SEM_100);
            storage_pvCenter.setShadowLayer(8f, 0f, 4f, COM_100);
        }

        // =========================================================
        // TEMAS
        // =========================================================
        if (current.type == SectionType.TEMAS) {

            final List<String> displayItems =
                    current.items != null ? current.items : Collections.emptyList();

            HorizontalScrollView hScroll = new HorizontalScrollView(this);
            hScroll.setId(R.id.hScroll);
            hScroll.setClipToPadding(false);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(0, 15, 0, 0);
            hScroll.addView(layout);

            int selectedIndex = prefsHelper.getLastThemePosition();

            int[] colorsTemas = {
                    ContextCompat.getColor(this, R.color.PADRAO_COM_100),
                    ContextCompat.getColor(this, R.color.ANEMO_COM_100),
                    ContextCompat.getColor(this, R.color.GEO_COM_100),
                    ContextCompat.getColor(this, R.color.ELECTRO_COM_100),
                    ContextCompat.getColor(this, R.color.DENDRO_COM_100),
                    ContextCompat.getColor(this, R.color.HYDRO_COM_100),
                    ContextCompat.getColor(this, R.color.PRYO_COM_100),
                    ContextCompat.getColor(this, R.color.CRYO_COM_100)
            };

            for (int i = 0; i < displayItems.size(); i++) {

                String label = displayItems.get(i);
                final int index = i;

                TextView tv = new TextView(this);
                tv.setText(label);
                tv.setTypeface(getResources().getFont(R.font.new_font), Typeface.BOLD);
                tv.setGravity(Gravity.CENTER);
                tv.setSingleLine(true);
                tv.setTextSize(21);
                tv.setPadding(20, 0, 20, 0);
                tv.setFocusable(true);

                int itemWidth = 190;
                if ("PADRÃƒO".equals(label)) itemWidth = 210;
                else if ("ANEMO".equals(label)) itemWidth = 200;
                else if ("GEO".equals(label)) itemWidth = 160;
                else if ("ELECTRO".equals(label)) itemWidth = 230;
                else if ("DENDRO".equals(label)) itemWidth = 210;
                else if ("HYDRO".equals(label)) itemWidth = 210;

                LinearLayout.LayoutParams lp =
                        new LinearLayout.LayoutParams(itemWidth, 72);
                lp.setMargins(6, 0, 6, 0);
                tv.setLayoutParams(lp);

                boolean isSelected = (index == selectedIndex);

                tv.setBackgroundColor(isSelected ? COM_100 : SEM_100);
                tv.setTextColor(isSelected ? BLACK_100 : COM_100);

                if (isSelected) {
                    tv.post(tv::requestFocus);
                }

                tv.setOnFocusChangeListener((v, hasFocus) -> {

                    tv.setSelected(hasFocus);

                    if (hasFocus) {
                        tv.setBackgroundColor(COM_100);

                        int cor = colorsTemas[Math.min(index, colorsTemas.length - 1)];
                        tv.setTextColor(cor == COM_100 ? BLACK_100 : cor);

                        centerItemInCarousel(hScroll, v);

                    } else {
                        boolean stillSelected =
                                prefsHelper.getLastThemePosition() == index;

                        tv.setBackgroundColor(stillSelected ? COM_080 : SEM_100);
                        tv.setTextColor(stillSelected ? BLACK_080 : COM_100);
                    }
                });

                tv.setOnClickListener(v -> {
                    prefsHelper.setLastThemePosition(index);
                    aplicarTema(index);
                    listaVertical();
                });

                layout.addView(tv);
            }

            storage_actvContent.addView(hScroll);
            return;
        }

        // =========================================================
        // STORAGE (COM CACHE SEGURO)
        // =========================================================

        if (current.storage != null) {
            String storagePath = current.storage.getAbsolutePath();

            if (current.getCurrentPath() == null ||
                    !current.getCurrentPath().startsWith(storagePath)) {
                current.goTo(storagePath);
            }
        }

        final String path = current.getCurrentPath();

        // 1. Busca os itens no Cache de forma segura
        List<FolderItem> cachedItems = folderCache.get(path);
        List<FolderItem> items;

        if (cachedItems != null) {
            // ðŸ”¥ RETORNO INSTANTÃ‚NEO: Cria uma cÃ³pia defensiva para evitar que
            // alteraÃ§Ãµes na interface alterem ou corrompam o cache original.
            items = new ArrayList<>(cachedItems);
        } else {
            // 2. Carga fÃ­sica do repositÃ³rio (Caso nÃ£o exista no cache)
            List<FolderItem> loadedItems = MediaRepository.loadFolder(this, path);

            if (loadedItems != null && !loadedItems.isEmpty()) {
                items = new ArrayList<>(loadedItems);

                // 3. ORDENAÃ‡ÃƒO OTIMIZADA: Evita conversÃµes repetidas de String dentro do loop.
                // Usar a estrutura interna do Java reduz em atÃ© 4x o tempo de processamento em pastas gigantes.
                items.sort((a, b) -> {
                    String nameA = a.name != null ? a.name : "";
                    String nameB = b.name != null ? b.name : "";
                    return nameA.compareToIgnoreCase(nameB);
                });

                // Salva a cÃ³pia ordenada de forma definitiva no cache
                folderCache.put(path, new ArrayList<>(items));
            } else {
                items = Collections.emptyList();
            }
        }

        // =========================================================
        // FIX CRÃTICO: render seguro (PrÃ³ximo bloco que vocÃª enviou antes)
        // =========================================================
        boolean needsRender = !Objects.equals(path, lastRenderedPath)
                || storage_actvContent == null
                || storage_actvContent.getChildCount() == 0;

        if (!needsRender) return;

        lastRenderedPath = path;

        if (items.isEmpty()) {
            renderizarInterface(Collections.emptyList(), path);
            return;
        }

        // ConversÃ£o segura dos itens para a interface do Explorer
        final List<ExplorerItem> explorerItems = new ArrayList<>(items.size());
        for (FolderItem fi : items) {
            if (fi != null) {
                explorerItems.add(new LocalItem(fi));
            }
        }

        // ExecuÃ§Ã£o segura na Thread principal controlando a interatividade do hardware
        if (!isFinishing() && !isDestroyed() && storage_actvContent != null) {

            // ðŸ”¥ TRAVA ANTIDÃšPLO CLIQUE: Desativa a interatividade do container durante o processamento pesado
            storage_actvContent.setClickable(false);
            storage_actvContent.setFocusable(false);

            // Injeta os dados na Ã¡rvore vertical (Ativa o Shimmer/Fantasmas internamente)
            renderizarInterface(explorerItems, path);

            // Libera o container apÃ³s o tempo de renderizaÃ§Ã£o (MENU_RENDER)
            storage_actvContent.postDelayed(() -> {
                if (isFinishing() || isDestroyed() || storage_actvContent == null) return;

                storage_actvContent.setClickable(true);
                storage_actvContent.setFocusable(true);

            }, MENU_RENDER);
        }

    }


    // ===============================
    // RENDERIZAÃ‡ÃƒO DE ITENS
    // ===============================
    private ShimmerEfeito shimmerEfeito = new ShimmerEfeito();

    // Guarda temporariamente o foco de cada pasta enquanto o usuÃ¡rio navega na tela
    private final Map<String, Integer> memoriaFocoPorLinha = new HashMap<>();

    @SuppressLint("SetTextI18n")
    private void renderizarInterface(List<ExplorerItem> items, final String path) {
        if (isFinishing() || isDestroyed()) return;

        if (sections == null || sections.isEmpty() || currentIndex < 0 || currentIndex >= sections.size()) {
            Log.e("RENDER_UI", "SeÃ§Ãµes invÃ¡lidas.");
            return;
        }

        // 1. Limpa agendamentos e instÃ¢ncias antigas
        if (renderItemsRunnable != null) {
            storage_actvContent.removeCallbacks(renderItemsRunnable);
        }
        shimmerEfeito.parar();
        storage_actvContent.removeAllViews();

        final MenuSection current = sections.get(currentIndex);
        if (current.type == SectionType.TEMAS) return;

        // ðŸ”¥ CRÃTICO: Criamos um contÃªiner absoluto de camadas para empilhar o Real sob o Fantasma
        FrameLayout wrapperCamadas = new FrameLayout(this);
        storage_actvContent.addView(wrapperCamadas);

        // ContÃªiner linear horizontal exclusivo para manter os esqueletos alinhados
        LinearLayout ghostContainer = new LinearLayout(this);
        ghostContainer.setOrientation(LinearLayout.HORIZONTAL);
        wrapperCamadas.addView(ghostContainer);

        // 2. CriaÃ§Ã£o dos Ghosts (Skeletons estruturais)
        int totalFantasmas = Math.min(items != null ? items.size() : 0, 7);
        if (totalFantasmas == 0) totalFantasmas = 3;

        final List<View> listaGhosts = new ArrayList<>();
        final LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < totalFantasmas; i++) {
            View ghostItem = inflater.inflate(R.layout.main_sombras, ghostContainer, false);
            ghostContainer.addView(ghostItem);
            listaGhosts.add(ghostItem);
        }

        // 3. INICIA O SHIMMER PROFISSIONAL (Nativo e fluido na GPU)
        shimmerEfeito.iniciar(listaGhosts, COM_020, COM_040);

        // 4. PREPARA OS DADOS REAIS NO RUNNABLE
        renderItemsRunnable = () -> {
            if (isFinishing() || isDestroyed()) return;

            if (sections.get(currentIndex).type == SectionType.TEMAS) {
                storage_actvContent.removeAllViews();
                shimmerEfeito.parar();
                return;
            }

            if (items == null || items.isEmpty()) {
                shimmerEfeito.parar();
                storage_actvContent.removeAllViews();
                return;
            }

            // Criando a estrutura real escondida (Alpha 0)
            HorizontalScrollView hScroll = new HorizontalScrollView(this);
            hScroll.setId(R.id.hScroll);
            hScroll.setClipToPadding(false);
            hScroll.setAlpha(0f); // ComeÃ§a invisÃ­vel

            LinearLayout foldersLayout = new LinearLayout(this);
            foldersLayout.setOrientation(LinearLayout.HORIZONTAL);
            hScroll.addView(foldersLayout);

            GradientDrawable gradientFocus = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM, new int[]{COM_020, COM_080, COM_100});
            GradientDrawable gradientNormal = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM, new int[]{SEM_020, SEM_080, SEM_100});

            // --- INÃCIO DO LOOP COMPLETO DE ITENS ---
            for (int i = 0; i < items.size(); i++) {
                final ExplorerItem item = items.get(i);
                if (item == null) continue;

                final int index = i;
                final String name = item.name;
                final String fullPath = item.path;
                final boolean isFolder = item.isFolder();
                final Uri uri = (item instanceof LocalItem) ? ((LocalItem) item).uri : null;

                View folderView = inflater.inflate(R.layout.main_items, foldersLayout, false);

                MaterialCardView card = folderView.findViewById(R.id.cardView);
                RoundedImageView thumb = folderView.findViewById(R.id.cardImage);
                LinearLayout cardUpdown = folderView.findViewById(R.id.mini_updown);

                TextView title = folderView.findViewById(R.id.title);
                TextView subtitle = folderView.findViewById(R.id.subtitle);

                FrameLayout cardShadow = folderView.findViewById(R.id.cardSombra);
                FrameLayout pgsView = folderView.findViewById(R.id.mini_view);

                ProgressBar progress = folderView.findViewById(R.id.mini_pgsb);
                ProgressBar pgsLoad = folderView.findViewById(R.id.mini_load);

                String status = prefsHelper.getLastVideoStatus(name + ".mp4");
                boolean isComplete = PrefsHelper.STATUS_COMPLETE.equals(status);

                card.setCardBackgroundColor(SEM_100);
                cardShadow.setBackgroundColor(COM_020);

                cardUpdown.getLayoutParams().height = 0;
                cardUpdown.setBackgroundColor(COM_040);
                cardUpdown.requestLayout();

                pgsLoad.setIndeterminateTintList(ColorStateList.valueOf(COM_100));
                pgsLoad.setBackground(ContextCompat.getDrawable(this, ICON));
                pgsView.setBackground(ContextCompat.getDrawable(this, THUMBR));
                if (pgsView.getBackground() != null)
                    pgsView.getBackground().setAlpha(51);

                pgsView.setVisibility(View.VISIBLE);

                title.setText(name);
                title.setTextColor(COM_080);
                subtitle.setTextColor(COM_080);

                if (isFolder) {
                    bindFolder(subtitle, progress, ((LocalItem) item).source);

                    File folder = new File(current.getCurrentPath());
                    File seasonImage = new File(folder, name + "/" + name + ".png");

                    if (!seasonImage.exists()) {
                        seasonImage = new File(folder, name + "/" + name + ".jpg");
                    }

                    Glide.with(this)
                            .load(seasonImage)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .apply(new RequestOptions().transform(new CenterCrop()))
                            .listener(new RequestListener<>() {
                                private void fechar() { pgsView.setVisibility(View.GONE); }
                                @Override public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) { fechar(); return false; }
                                @Override public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) { fechar(); return false; }
                            })
                            .error(THUMBR)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .skipMemoryCache(false)
                            .into(thumb);

                } else {
                    bindVideo(card, subtitle, progress, thumb, ((LocalItem) item).source);

                    if (uri != null) {
                        Glide.with(this)
                                .load(uri)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .apply(new RequestOptions().transform(new CenterCrop()))
                                .listener(new RequestListener<>() {
                                    private void fechar() { pgsView.setVisibility(View.GONE); }
                                    @Override public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) { fechar(); return false; }
                                    @Override public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) { fechar(); return false; }
                                })
                                .error(THUMBR)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .skipMemoryCache(false)
                                .priority(Priority.NORMAL)
                                .thumbnail(0.85f)
                                .into(thumb);
                    } else {
                        pgsView.setVisibility(View.GONE);
                    }
                }

                // LÃ“GICA DE FOCO DOS ITENS
                folderView.setFocusable(true);

                folderView.setOnFocusChangeListener((v, hasFocus) -> {
                    File fileKey = new File(fullPath);
                    String fName = fileKey.getParentFile() != null ? fileKey.getParentFile().getName() : "Root";
                    String cleanT = name.replace(".mp4", "").replace(".MP4", "");
                    String itemKey = fName + "/" + cleanT + ".mp4";

                    String currentStatus = prefsHelper.getLastVideoStatus(itemKey);
                    boolean itemIsComplete = !isFolder && PrefsHelper.STATUS_COMPLETE.equals(currentStatus);

                    int targetDip = isFolder ? 0 : (hasFocus ? 4 : 0);
                    int badgeTargetHeight = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, targetDip, v.getResources().getDisplayMetrics());

                    cardUpdown.animate().cancel();
                    int currentHeight = cardUpdown.getHeight();

                    if (currentHeight != badgeTargetHeight) {
                        ValueAnimator badgeAnim = ValueAnimator.ofInt(currentHeight, badgeTargetHeight);
                        badgeAnim.setDuration(200);
                        badgeAnim.addUpdateListener(a -> {
                            cardUpdown.getLayoutParams().height = (int) a.getAnimatedValue();
                            cardUpdown.requestLayout();
                        });
                        badgeAnim.start();
                    }

                    if (hasFocus) {
                        // ðŸŽ¯ AGORA SIM: O mÃ©todo usa o 'path' real que veio por parÃ¢metro!
                        String chaveLinha = (path != null && !path.trim().isEmpty()) ? path : "root_default";

                        // Grava a coluna atual na memÃ³ria RAM usando o path estÃ¡vel
                        memoriaFocoPorLinha.put(chaveLinha, index);

                        current.lastFocusedPosition = index;
                        v.post(() -> centerItemInCarousel(hScroll, v));

                        // Preview corrigido e blindado contra nulidades usando a chave estÃ¡vel
                        String previewPath = isFolder ?
                                path + File.separator + name + File.separator + name + ".png" :
                                fullPath;
                        onItemFocused(name, previewPath);

                        card.setCardBackgroundColor(COM_100);
                        cardUpdown.setBackgroundColor(itemIsComplete ? Color.argb(120, 0, 0, 0) : COM_040);
                        title.setTextColor(BLACK_100);
                        subtitle.setTextColor(BLACK_080);
                        thumb.setColorFilter(null);

                        progress.setProgressTintList(ColorStateList.valueOf(SEM_100));
                        if (!isFolder) progress.setVisibility(View.VISIBLE);

                    } else {
                        title.setTextColor(COM_080);
                        subtitle.setTextColor(WHITE_080);

                        if (itemIsComplete) {
                            card.setCardBackgroundColor(Color.argb(120, 0, 0, 0));
                            thumb.setColorFilter(Color.argb(120, 0, 0, 0));
                        } else {
                            card.setCardBackgroundColor(SEM_100);
                            thumb.setColorFilter(null);
                        }

                        cardUpdown.setBackgroundColor(SEM_040);

                        if (!isFolder) {
                            long p = prefsHelper.getLastVideoProgress(itemKey);
                            progress.setVisibility((itemIsComplete || p > 0) ? View.VISIBLE : View.GONE);
                            progress.setProgressTintList(ColorStateList.valueOf(itemIsComplete ? COM_020 : SEM_080));
                        }
                    }

                    title.setSelected(hasFocus);
                    subtitle.setSelected(hasFocus);
                });

                // ðŸ”¥ 2. TRAVA MECÃ‚NICA EXCLUSIVA PARA NAVEGAÃ‡ÃƒO VERTICAL (SOBE/DESCE)
                folderView.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {

                            // ðŸ”¥ TRAVA 1: SÃ³ salva no fÃ­sico se o usuÃ¡rio estiver alÃ©m do primeiro item (> 0)
                            // Isso impede que reconstruÃ§Ãµes fantasmas em background limpem o foco real
                            if (index > 0) {
                                String chaveLinha = (path != null && !path.trim().isEmpty()) ? path : "root_default";
                                prefsHelper.setLastLinePosition(chaveLinha, index);

                                android.util.Log.d("TESTE_FOCO", "MecÃ¢nico: Gravou linha [" + chaveLinha + "] na coluna: " + index);
                            }
                        }
                    }
                    return false;
                });

                folderView.setOnClickListener(v -> {
                    if (isFolder) {
                        current.goTo(fullPath);
                        listaVertical();
                    } else {
                        listaVerticalDescer(storageContainer, mGradient, () -> abrirPlayer(name, fullPath, items));
                    }
                });

                foldersLayout.addView(folderView);
            }

            // ðŸ”„ FIM DO LOOP DE ITENS

            // =========================================================
            // TRANSIÃ‡ÃƒO DE CAMADAS E RESTAURAÃ‡ÃƒO DE FOCO
            // =========================================================
            wrapperCamadas.addView(hScroll, 0);

            hScroll.post(() -> {
                if (isFinishing() || isDestroyed()) return;

                ghostContainer.animate().alpha(0f).setDuration(280).start();

                hScroll.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            if (isFinishing() || isDestroyed()) return;

                            shimmerEfeito.parar();
                            wrapperCamadas.removeView(ghostContainer);
                            wrapperCamadas.removeView(hScroll);
                            storage_actvContent.removeAllViews();
                            storage_actvContent.addView(hScroll);

                            final String chaveLinha = (path != null && !path.trim().isEmpty()) ? path : "root_default";
                            int targetPos;

                            if (prefsHelper.isFromPlayer()) {
                                // ðŸŽ¬ CASO A: Se veio do player, obedece a posiÃ§Ã£o do vÃ­deo
                                targetPos = prefsHelper.getLastVideoPosition();
                                prefsHelper.setFromPlayer(false); // Consome a flag imediatamente
                                prefsHelper.setLastLinePosition(chaveLinha, targetPos);
                            } else {
                                // ðŸ§­ CASO B: NavegaÃ§Ã£o comum Sobe e Desce
                                int posicaoSalva = prefsHelper.getLastLinePosition(chaveLinha);

                                // ðŸ”¥ TRAVA 2: Se o fÃ­sico foi zerado incorretamente por um reset, mas o objeto
                                // 'current' guardou o foco vÃ¡lido na RAM antes de sumir, usamos ele como backup.
                                if (posicaoSalva == 0 && current != null && current.lastFocusedPosition > 0) {
                                    targetPos = current.lastFocusedPosition;
                                } else {
                                    targetPos = posicaoSalva;
                                }

                                android.util.Log.d("TESTE_FOCO", "FÃ­sico Recuperado para [" + chaveLinha + "] -> Coluna: " + targetPos);
                            }

                            final int childCount = foldersLayout.getChildCount();
                            if (childCount > 0) {
                                // ProteÃ§Ã£o de limites fÃ­sicos do array
                                int finalPos = Math.max(0, Math.min(targetPos, childCount - 1));
                                final View viewToFocus = foldersLayout.getChildAt(finalPos);

                                if (viewToFocus != null) {
                                    viewToFocus.post(() -> {
                                        if (isFinishing() || isDestroyed()) return;

                                        foldersLayout.setFocusable(false);
                                        viewToFocus.setFocusable(true);

                                        // Cravando o foco na view correta
                                        boolean success = viewToFocus.requestFocus();
                                        if (success && hScroll instanceof HorizontalScrollView) {
                                            centerItemInCarousel(hScroll, viewToFocus);
                                        }
                                    });
                                }
                            }
                        }).start();
            });

        };

        // Mantemos o gatilho inicial para os esqueletos nascerem na tela primeiro
        storage_actvContent.postDelayed(renderItemsRunnable, 250L);
    }



    // ===============================
    // PREVIEWS DE DISPOSITIVO
    // ===============================
    private void updatePreviews() {

        if (sections == null || sections.isEmpty()) return;

        // =========================
        // TOP (anterior)
        // =========================
        if (storage_pvTop != null) {

            if (currentIndex > 0) {

                MenuSection prev = sections.get(currentIndex - 1);

                storage_pvTop.setText(prev.getDisplayName());
                storage_pvTop.setPadding(5, 15, 0, 0);

                storage_pvTop.setShadowLayer(
                        8f,
                        0f,
                        4f,
                        COM_040
                );

                storage_pvTop.setVisibility(View.VISIBLE);
                storage_pvTop.setTextColor(SEM_040);
                storage_pvTop.setAlpha(0.25f);

            } else {

                storage_pvTop.setVisibility(View.GONE);

                // reset total (evita vazamento de estado)
                storage_pvTop.setAlpha(1f);
                storage_pvTop.setTextColor(Color.WHITE);
                storage_pvTop.setShadowLayer(0f, 0f, 0f, 0);
                //storage_pvTop.setPadding(0, 0, 0, 0);
            }
        }

        // =========================
        // BOTTOM (prÃ³ximo)
        // =========================
        if (storage_pvBottom != null) {

            if (currentIndex < sections.size() - 1) {

                MenuSection next = sections.get(currentIndex + 1);

                storage_pvBottom.setText(next.getDisplayName());
                storage_pvBottom.setPadding(5, 0, 0, 15);

                storage_pvBottom.setShadowLayer(
                        8f,
                        0f,
                        4f,
                        COM_040
                );

                storage_pvBottom.setVisibility(View.VISIBLE);
                storage_pvBottom.setTextColor(SEM_040);
                storage_pvBottom.setAlpha(0.5f);

            } else {

                storage_pvBottom.setVisibility(View.INVISIBLE);

                storage_pvBottom.setAlpha(1f);
                storage_pvBottom.setTextColor(Color.WHITE);
                storage_pvBottom.setShadowLayer(0f, 0f, 0f, 0);
                //storage_pvBottom.setPadding(0, 0, 0, 0);
            }
        }
    }


    // ===============================
    // PLAYER
    // ===============================
    private void abrirPlayer(String nome, String path, List<ExplorerItem> items) {
        if (items == null || items.isEmpty()) return;
        if (path == null || path.isEmpty()) return;

        ArrayList<VideoFile> videoList = new ArrayList<>();

        for (ExplorerItem item : items) {
            if (item == null || item.isFolder()) continue;

            if (item instanceof LocalItem) {
                FolderItem f = ((LocalItem) item).source;

                if (f != null && f.fullPath != null) {

                    Uri videoUri = (f.uri != null)
                            ? f.uri
                            : Uri.fromFile(new File(f.fullPath));

                    String name = (f.name != null) ? f.name : "video";
                    String thumbPath = getThumbPath(item);

                    videoList.add(new VideoFile(
                            name,
                            f.fullPath,
                            videoUri,
                            0,
                            thumbPath
                    ));
                }
            }
        }

        if (videoList.isEmpty()) return;

        // OrdenaÃ§Ã£o
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            videoList.sort((a, b) -> {
                String a1 = a.getTitle() != null ? a.getTitle() : "";
                String b1 = b.getTitle() != null ? b.getTitle() : "";
                return a1.compareToIgnoreCase(b1);
            });
        }

        // PosiÃ§Ã£o atual
        int position = 0;
        for (int i = 0; i < videoList.size(); i++) {
            if (videoList.get(i).getPath().equalsIgnoreCase(path)) {
                position = i;
                break;
            }
        }

        if (nome != null) {
            PrefsHelper.setLastVideoName(nome);
        }

        Intent intent = new Intent(this, MainPlayer.class);
        intent.putExtra("position", position);
        intent.putParcelableArrayListExtra("videoArrayList", videoList);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        startActivity(
                intent,
                ActivityOptions.makeCustomAnimation(
                        this,
                        R.anim.transition_in,
                        R.anim.transition_out
                ).toBundle()
        );
    }

    private String getThumbPath(ExplorerItem item) {
        if (item == null) return null;

        try {
            String name = item.name;
            if (name == null || name.isEmpty()) return null;

            if (item instanceof LocalItem) {

                FolderItem f = ((LocalItem) item).source;
                if (f == null || f.fullPath == null) return null;

                File videoFile = new File(f.fullPath);
                File baseFolder = videoFile.getParentFile();

                if (baseFolder == null || !baseFolder.exists()) return null;

                // tenta PNG
                File png = new File(baseFolder, name + ".png");
                if (png.exists()) return png.getAbsolutePath();

                // tenta JPG
                File jpg = new File(baseFolder, name + ".jpg");
                if (jpg.exists()) return jpg.getAbsolutePath();

                // ðŸ”¥ fallback inteligente (caso nome nÃ£o bata 100%)
                File[] files = baseFolder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String fileName = file.getName().toLowerCase();

                        if (fileName.endsWith(".png") || fileName.endsWith(".jpg")) {
                            if (fileName.contains(name.toLowerCase())) {
                                return file.getAbsolutePath();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // ===============================
    // BIND DE ITENS LOCAIS
    // ===============================
    private void bindFolder(TextView subtitle, ProgressBar progress, FolderItem item) {
        progress.setVisibility(View.GONE);
        StringBuilder info = new StringBuilder();
        if (item != null) {
            if (item.folderCount > 0) info.append(item.folderCount).append(" pastas");
            if (item.videoCount > 0) {
                if (info.length() > 0) info.append(" â€¢ ");
                info.append(item.videoCount).append(" vÃ­deos");
            }
        }
        subtitle.setText(info.toString());
    }

    @SuppressLint("SetTextI18n")
    private void bindVideo(CardView card, TextView subtitle, ProgressBar progress, ImageView thumb, FolderItem item) {

        // 1. RESET RADICAL (Essencial para nÃ£o herdar cores de itens anteriores no scroll)
        if (thumb != null) thumb.setColorFilter(null);
        if (card != null) card.setCardBackgroundColor(SEM_100);
        progress.setVisibility(View.GONE);
        progress.setProgress(0);
        subtitle.setText("");

        if (item == null) {
            if (card != null) card.setCardBackgroundColor(Color.TRANSPARENT);
            return;
        }

        // 2. NOVA CHAVE SEGURA (Pasta/Arquivo.mp4) - SINCRONIZADA COM O PLAYER
        File file = new File(item.fullPath);
        String folderName = file.getParentFile() != null ? file.getParentFile().getName() : "Root";

        // Limpa o nome para evitar o erro de ".mp4.mp4" e bater com a chave do onPause
        String cleanTitle = item.name.replace(".mp4", "").replace(".MP4", "");
        String key = folderName + "/" + cleanTitle + ".mp4";

        // 3. RECUPERAÃ‡ÃƒO DE DADOS
        String status = prefsHelper.getLastVideoStatus(key);
        long p = prefsHelper.getLastVideoProgress(key);
        long d = item.durationMs;

        // SÃ³ Ã© completo se o status for EXATAMENTE a constante
        boolean isComplete = PrefsHelper.STATUS_COMPLETE.equals(status);
        boolean hasProgress = p > 0 && !isComplete;

        subtitle.setText("TEMPO â€¢ " + formatDuration(d));

        // 4. LÃ“GICA DE INTERFACE
        if (isComplete) {
            // VÃDEO CONCLUÃDO
            progress.setVisibility(View.VISIBLE);
            progress.setProgress(100);
            progress.setProgressTintList(ColorStateList.valueOf(COM_020));

            // Escurece o card e a thumb
            if (card != null) card.setCardBackgroundColor(Color.argb(120, 0, 0, 0));
            if (thumb != null) thumb.setColorFilter(Color.argb(120, 0, 0, 0));

            Log.d("bindVideo", "âœ… COMPLETO: " + key);

        } else if (hasProgress) {
            // VÃDEO EM ANDAMENTO
            int percent = (d > 0) ? (int) ((p * 100L) / d) : 0;
            percent = Math.max(1, Math.min(99, percent));

            progress.setVisibility(View.VISIBLE);
            progress.setProgress(percent);
            progress.setProgressTintList(ColorStateList.valueOf(SEM_080));

            Log.d("bindVideo", "â³ PROGRESSO: " + key + " (" + percent + "%)");

        } else {
            // VÃDEO NOVO (O segundo dispositivo cairÃ¡ aqui, pois a chave composta nÃ£o existe no banco dele)
            Log.d("bindVideo", "ðŸ†• NOVO: " + key);
        }
    }

    @SuppressLint("DefaultLocale")
    private String formatDuration(long ms) {
        long s = ms / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return h > 0
                ? String.format("%d:%02d:%02d", h, m, sec)
                : String.format("%02d:%02d", m, sec);
    }

    // ===============================
    // CARROSSEL / SCROLL
    // ===============================
    private void centerItemInCarousel(HorizontalScrollView hsv, View targetView) {
        if (hsv == null || targetView == null) return;

        hsv.post(() -> {
            int itemLeft = targetView.getLeft();
            int itemWidth = targetView.getWidth();
            int hsvWidth = hsv.getWidth();

            if (hsvWidth <= 0 || hsv.getChildCount() == 0) return;

            int scrollToX = itemLeft + itemWidth / 2 - hsvWidth / 2;

            int maxScroll = hsv.getChildAt(0).getWidth() - hsvWidth;
            scrollToX = Math.max(0, Math.min(scrollToX, maxScroll));

            // ðŸ”¥ animaÃ§Ã£o suave
            hsv.smoothScrollTo(scrollToX, 0);
        });
    }


    // ===============================
    // BACKGROUND / PREVIEW
    // ===============================

    // ==========================================
    // VariÃ¡veis de Controle (Topo da Classe)
    // ==========================================
    private boolean isFrontVisible = true;

    // ==========================================
    // 1. Gerenciamento de Foco e Cache Preditivo
    // ==========================================
    @Override
    public void onItemFocused(String titulo, String imagePath) {
        if (isFinishing() || isDestroyed()) return;

        // Se o caminho da imagem for exatamente o mesmo, ignora
        if (Objects.equals(imagePath, lastImagePath)) {
            return;
        }

        lastImagePath = imagePath;

        // Cancela o agendamento anterior (Debounce)
        if (focusRunnable != null) {
            mainHandler.removeCallbacks(focusRunnable);
        }

        // ðŸ”¥ JOGADA PROFISSIONAL: PrÃ©-carrega esta imagem imediatamente no cache de disco.
        // Como o usuÃ¡rio acabou de focar, o Glide jÃ¡ vai baixando em background enquanto o
        // BACKGROUND_DELAY conta o tempo. Se ele parar aqui, a imagem jÃ¡ estarÃ¡ pronta!
        if (imagePath != null && !imagePath.trim().isEmpty()) {
            precarregarNoCache(imagePath);
        }

        final String requestedPath = imagePath;

        focusRunnable = () -> {
            if (isFinishing() || isDestroyed()) return;

            // Garante que o usuÃ¡rio nÃ£o mudou de item enquanto o Handler esperava
            if (!Objects.equals(requestedPath, lastImagePath)) {
                return;
            }

            // Se o caminho for vazio, faz o fade-out total dos backgrounds
            if (requestedPath == null || requestedPath.isEmpty()) {
                lastResolvedPath = null;
                fadeoutBackground();
            } else {
                // Resolve o caminho (Url, Arquivo ou Placeholder)
                Object novoCaminho = resolveImagePath(requestedPath);

                // TRAVA MÃGICA: Se o destino final for igual ao que jÃ¡ estÃ¡ na tela, ignora a transiÃ§Ã£o
                if (Objects.equals(novoCaminho, lastResolvedPath)) {
                    return;
                }

                lastResolvedPath = novoCaminho;
                configurarMidia(novoCaminho);
            }
        };

        // Dispara o timer para aplicar o background
        mainHandler.postDelayed(focusRunnable, BACKGROUND_DELAY);
    }

    // ==========================================
    // 2. TransiÃ§Ã£o CinemÃ¡tica (Cross-Fade Manteiga)
    // ==========================================
    private void configurarMidia(Object caminhoFinal) {
        if (isFinishing() || isDestroyed()) return;

        // Cancela animaÃ§Ãµes em curso para evitar sobreposiÃ§Ã£o de estados e travamento visual
        imgBackgroundFront.animate().cancel();
        imgBackgroundBack.animate().cancel();

        // Define quem entra (target) e quem sai (current) com base no estado ATUAL
        final ImageView targetView = isFrontVisible ? imgBackgroundBack : imgBackgroundFront;
        final ImageView currentView = isFrontVisible ? imgBackgroundFront : imgBackgroundBack;

        Glide.with(this)
                .load(caminhoFinal)
                .priority(Priority.IMMEDIATE) // Prioridade mÃ¡xima para o item focado
                .override(1280, 720)          // ForÃ§a o tamanho do cache (deve ser idÃªntico ao preload)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Salva imagem original + redimensionada
                .dontAnimate()                // Desativa animaÃ§Ã£o interna do Glide para nÃ£o conflitar com a nossa
                .placeholder(currentView.getDrawable()) // Usa o fundo atual como placeholder (evita tela branca)
                .centerCrop()
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        if (isFinishing() || isDestroyed()) return;

                        // CORREÃ‡ÃƒO CRÃTICA: Inverte a flag IMEDIATAMENTE aqui.
                        // Se o usuÃ¡rio mover o foco rÃ¡pido, o prÃ³ximo ciclo jÃ¡ pegarÃ¡ as views invertidas corretamente.
                        isFrontVisible = !isFrontVisible;

                        // Prepara a imagem que vai entrar (invisÃ­vel abaixo da atual)
                        targetView.setImageDrawable(resource);
                        targetView.setAlpha(0f);
                        targetView.setVisibility(View.VISIBLE);

                        // AnimaÃ§Ã£o da imagem que ENTRA (Fade-In)
                        targetView.animate()
                                .alpha(1f)
                                .setDuration(800) // 800ms cinematogrÃ¡fico
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .start();

                        // AnimaÃ§Ã£o da imagem que SAI (Fade-Out)
                        currentView.animate()
                                .alpha(0f)
                                .setDuration(800)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .withEndAction(() -> {
                                    // Limpeza pÃ³s-transiÃ§Ã£o: Libera memÃ³ria RAM limpando o bitmap oculto
                                    currentView.setImageDrawable(null);
                                    currentView.setVisibility(View.GONE);
                                })
                                .start();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Regra do Glide: Se o ciclo de vida destruir a requisiÃ§Ã£o, limpe a view
                        targetView.setImageDrawable(null);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        if (isFinishing() || isDestroyed()) return;
                        // Em caso de erro severo, define o drawable de erro suavemente na target
                        if (errorDrawable != null) {
                            targetView.setImageDrawable(errorDrawable);
                        }
                    }
                });
    }

    // ==========================================
    // 3. FunÃ§Ãµes Auxiliares e EstratÃ©gia de Cache
    // ==========================================

    /**
     * ForÃ§a o Glide a baixar a imagem direto para o cache de disco em segundo plano.
     * Deve possuir EXATAMENTE os mesmos parÃ¢metros de transformaÃ§Ã£o do configurarMidia.
     */
    private void precarregarNoCache(String imagePath) {
        Object resolvedPath = resolveImagePath(imagePath);

        Glide.with(this)
                .load(resolvedPath)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(1280, 720)
                .centerCrop()
                .preload(); // Baixa silenciosamente sem precisar de uma ImageView
    }

    private void fadeoutBackground() {
        imgBackgroundFront.animate().cancel();
        imgBackgroundBack.animate().cancel();
        imgBackgroundFront.animate().alpha(0f).setDuration(BACKGROUND_FADE_OUT_MS).start();
        imgBackgroundBack.animate().alpha(0f).setDuration(BACKGROUND_FADE_OUT_MS).start();
    }

    private void preloadAndApply(String imagePath) {
        Object resolvedPath = resolveImagePath(imagePath);
        lastImagePath = (imagePath != null) ? imagePath : "";
        configurarMidia(resolvedPath);
    }

    private Object resolveImagePath(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return IMAGE;
        }

        if (imagePath.startsWith("http")) {
            return Uri.parse(imagePath);
        }

        File file = new File(imagePath);
        if (!file.exists()) {
            return IMAGE;
        }

        return imagePath;
    }


    // ===============================
    // TECLAS (LONG PRESS)
    // ===============================
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            WS_NULL = false;
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            WS_NULL = false;
            // NÃ£o chamamos listaVertical() aqui, entÃ£o nada acontece se segurar.
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isFinishing() || isDestroyed()) return true;

            final Dialog dialog = new Dialog(this, R.style.EndSheetTheme);
            @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.main_sair, null);
            if (view == null) return true;

            LinearLayout bg = view.findViewById(R.id.close_bg);
            LinearLayout sub = view.findViewById(R.id.close_sub);
            TextView title = view.findViewById(R.id.close_titulo);
            TextView subtitle = view.findViewById(R.id.close_titulo_sub);
            Button btnSim = view.findViewById(R.id.close_sim);
            Button btnNao = view.findViewById(R.id.close_nao);

            if (bg != null) bg.setBackgroundColor(COM_040);
            if (sub != null) sub.setBackgroundColor(SEM_080);

            dialog.setContentView(view);

            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams p = window.getAttributes();
                p.gravity = Gravity.CENTER;

                int w = WindowManager.LayoutParams.WRAP_CONTENT;
                int h = WindowManager.LayoutParams.WRAP_CONTENT;
                p.x = 50;
                p.y = 50;
                p.width = w;
                p.height = h;

                //window.setAttributes(p);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                float scale = w / 1280f;
                scale = Math.max(0.5f, Math.min(scale, 1.0f));

                // Mover para cima
                int deslocamentoParaCima = (int) (250 * scale);
                p.y = -deslocamentoParaCima;
                window.setAttributes(p);

                if (title != null) {
                    title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34 + (12 * scale));
                    title.setTextColor(COM_080);
                }

                if (subtitle != null) {
                    subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10 + (12 * scale));
                    subtitle.setTextColor(COM_080);
                }

                if (btnSim != null)
                    btnSim.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12 + (12 * scale));

                if (btnNao != null)
                    btnNao.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12 + (12 * scale));
            }

            int dp = (int) getResources().getDisplayMetrics().density;
            GradientDrawable base = new GradientDrawable();
            base.setShape(GradientDrawable.RECTANGLE);
            base.setStroke(4 * dp, COM_020);
            base.setColor(SEM_040);

            if (btnSim != null) {
                btnSim.setBackground(base);
                btnSim.setTextColor(COM_020);
            }

            if (btnSim != null && btnNao != null) {
                View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
                    GradientDrawable d = new GradientDrawable();
                    d.setShape(GradientDrawable.RECTANGLE);

                    int border = hasFocus ? BLACK_080 : COM_020;
                    d.setStroke(4 * dp, border);
                    d.setColor(hasFocus ? COM_080 : SEM_040);

                    v.setBackground(d);
                    if (v instanceof TextView) {
                        ((TextView) v).setTextColor(border);
                    }
                };

                btnSim.setOnFocusChangeListener(focusListener);
                btnNao.setOnFocusChangeListener(focusListener);

                btnSim.setOnClickListener(v -> {
                    dialog.dismiss();
                    finishAfterTransition();
                });

                btnNao.setOnClickListener(v -> dialog.dismiss());
            }

            dialog.show();
            if (btnNao != null) btnNao.requestFocus();

            return true;
        }

        return false;
    }

    // ===============================
    // TECLAS (DOWN / PREPARAÃ‡ÃƒO)
    // ===============================
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU ||
                keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            WS_NULL = true;
            return true;
        }

        if (sections == null || sections.isEmpty()) return super.onKeyDown(keyCode, event);

        // DPAD NavegaÃ§Ã£o (Mantido no Down para ser rÃ¡pido)
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            // 1. BLOQUEIO ABSOLUTO: Se jÃ¡ estiver processando um clique, descarta o prÃ³ximo
            if (isNavigating) return true;

            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                event.startTracking();

                if (event.getRepeatCount() == 0) {
                    isNavigating = true; // Ativa a trava imediatamente

                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP && currentIndex > 0) {
                        currentIndex--;
                        listaVertical();
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && currentIndex < sections.size() - 1) {
                        currentIndex++;
                        listaVertical();
                    }

                    // 2. LIBERAÃ‡ÃƒO CONTROLADA:
                    // Espera 400ms (tempo para a animaÃ§Ã£o e o carregamento dos itens estabilizarem)
                    new Handler(Looper.getMainLooper()).postDelayed(() -> isNavigating = false, MENU_RENDER);

                    return true;
                }
            }
        }


        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // APENAS prepara o rastreio. NÃƒO coloca lÃ³gica de voltar pasta aqui.
            event.startTracking();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    // ===============================
    // TECLAS (UP / CLIQUE CURTO)
    // ===============================
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        // Use || (OU) para que funcione com qualquer uma das teclas
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {

            // Reset simples: se soltou a tecla, a trava de "fim de lista" acaba
            WS_NULL = false;

            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (event.isTracking() && !event.isCanceled()) {

                // debounce (evita clique duplo rÃ¡pido)
                if (SystemClock.elapsedRealtime() - lastBackClickTime < 500) return true;
                lastBackClickTime = SystemClock.elapsedRealtime();

                MenuSection current = (sections != null && currentIndex >= 0)
                        ? sections.get(currentIndex)
                        : null;

                if (current != null) {
                    String currentPath = current.getCurrentPath();

                    if (currentPath != null && current.storage != null) {

                        File currentFolder = new File(currentPath);
                        File rootFolder = current.storage;

                        // se NÃƒO estÃ¡ na raiz â†’ volta uma pasta
                        if (!currentFolder.getAbsolutePath().equals(rootFolder.getAbsolutePath())) {

                            File parent = currentFolder.getParentFile();

                            if (parent != null) {
                                current.goTo(parent.getAbsolutePath());
                                listaVertical();
                                return true;
                            }
                        }
                    }
                }

                // ðŸ”¥ Aqui decide: sair do app ou nÃ£o
                // ou remove se quiser bloquear saÃ­da
            }

            return true;
        }
        return WS_NULL || super.onKeyUp(keyCode, event);
    }





    // ===============================
    // APLICA TEMA
    // ===============================
    private void aplicarTema(int position) {
        if (themeManager == null) {
            themeManager = new ThemeManager();
        }

        ThemeConfig config = themeManager.apply(this, position);
        THEME = config.themeName;

        if (imgBackgroundBack != null) {
            imgBackgroundBack.setImageResource(config.image);
            imgBackgroundBack.setAlpha(1f);
            lastResolvedPath = config.image;
            isFrontVisible = false;
        }

        if (imgBackgroundFront != null) {
            imgBackgroundFront.setAlpha(0f);
        }

        listaVertical();

        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

}
