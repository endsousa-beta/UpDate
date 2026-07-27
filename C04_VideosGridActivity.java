package my.endsousa.tv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import my.endsousa.tv.theme.AppTheme;
import my.endsousa.tv.theme.ThemeManager;

public class C04_VideosGridActivity extends FragmentActivity {


    private TextView txtExplorerTitle;
    private TextView txtExplorerBottom;
    // Elementos do Layout do Player e Fundo Fixo
    private View explorerBackground; // View de fundo independente gerada via Java
    private RelativeLayout layoutControlesPlayer;
    private TextView txtTempoAtual;
    private TextView txtTempoTotal;
    private ProgressBar barraProgressoVideo;
    private TextView txtIndicadorPause;
    private final Handler handlerProgresso = new Handler(Looper.getMainLooper());

    //private boolean isWallpaperAtivo = false;

    // Estados e Triggers do Sistema
    private boolean isBloqueador = false;
    private boolean isWallpaperAtivo = true; // Controla o botão LIGA/DESLIGA da tecla 1

    private static final long BACKGROUND_FADE_IN_MS = 250L;

    public static final int CONTAINER_FRAGMENT_ID = 9001;

    private PlaylistFragment playlistFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. PRIMEIRA LINHA OBRIGATÓRIA DO CICLO NATIVO
        super.onCreate(savedInstanceState);

        // Carrega preferências
        prefsHelper = new PrefsHelper(this);
        isWallpaperAtivo = prefsHelper.isFocusWallpaper();

        // 2. INFLA O XML DO SEU VIDEO_PLAYER DIRETAMENTE NA ACTIVITY
        setContentView(R.layout.activity_main);

        // =========================================================================
        // 3. INJEÇÃO BLINDADA DO EXPLORER_BACKGROUND VIA JANELA MESTRE (ÍNDICE 0)
        // =========================================================================
        // Vincula à janela invisível mestre que envelopa qualquer layout no Android
        ViewGroup rootWindow = findViewById(android.R.id.content);

        if (rootWindow != null) {
            // Instancia como uma View comum para casar com o seu CustomTarget do Glide
            explorerBackground = new View(this);

            FrameLayout.LayoutParams bgParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            explorerBackground.setLayoutParams(bgParams);

            // Força a exibição e injeta a cor de teste direto na estrutura nativa
            explorerBackground.setVisibility(View.VISIBLE);

            // TESTE REAL: Se a tela da TV ficar azul ao abrir o catálogo, o circuito destravou!
            explorerBackground.setBackgroundColor(Color.BLUE);

            // O ÍNDICE 0 força essa view a ficar atrás de TODOS os elementos do seu XML original
            rootWindow.addView(explorerBackground, 0);
        }
        // =========================================================================

        // 4. MAPEA OS IDs DO SEU XML COMUM (R.layout.video_player)
        menu_pvTop = findViewById(R.id.previewTop);
        menu_pvBottom = findViewById(R.id.previewBottom);
        menu_pvCenter = findViewById(R.id.activeTitle);
        menu_actvContent = findViewById(R.id.activeContent);
        menuContainer = findViewById(R.id.root_layout_down);
        mDownGradient = findViewById(R.id.root_gradient_down);
        containerPlaylist = findViewById(R.id.container_playlist);

        // 5. INICIALIZAÇÃO DO SEU FRAGMENTO DA PLAYLIST LEANBACK
        if (containerPlaylist != null) {
            ViewGroup.LayoutParams params = containerPlaylist.getLayoutParams();
            params.height = dp(166);
            containerPlaylist.setLayoutParams(params);
            containerPlaylist.setVisibility(View.VISIBLE);
        }

        if (savedInstanceState == null) {
            playlistFragment = new PlaylistFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container_playlist, playlistFragment)
                    .commit();
        } else {
            playlistFragment = (PlaylistFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.container_playlist);
        }

        // 6. CONFIGURAÇÕES ADICIONAIS DE INTERFACE
        setupSections();
        listaVerticalSubir(menu_actvContent, null);
        forçarFocoNaPlaylist();

        // Executa as renderizações de temas por cima da árvore montada
        //ThemeManager themeManager = new ThemeManager();
        //themeManager.load(this);
    }


    /**
     * Garante que o foco seja devolvido ao catálogo Leanback assim que a interface terminar de renderizar.
     */
    private void forçarFocoNaPlaylist() {
        if (containerPlaylist != null) {
            containerPlaylist.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed() && playlistFragment != null) {
                    View fragmentView = playlistFragment.getView();
                    if (fragmentView != null) {
                        fragmentView.requestFocus();
                        Log.d("FOCO", "Foco direcionado com sucesso para a Playlist.");
                    }
                }
            }, 150);
        }
    }

    private static final int MENU_ANIM_TIME = 500;

    private View menuGradient, mDownGradient;

    PrefsHelper prefsHelper;
    private RelativeLayout menuContainer, explorer_view;
    private LinearLayout explorer_content, menu_actvContent, layoutTabs, exo_lock_next_prev;
    private TextView menu_pvCenter,menu_pvTop, menu_pvBottom;




    public ArrayList<File> getStorages2() {

        ArrayList<File> roots = new ArrayList<>();

        StorageManager sm =
                (StorageManager) getSystemService(Context.STORAGE_SERVICE);

        if (sm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            for (StorageVolume vol : sm.getStorageVolumes()) {

                File dir = null;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    dir = vol.getDirectory();
                }

                if (dir != null && dir.exists()) {
                    roots.add(dir);
                }
            }
        }

        return roots;
    }
    public String getStorageType2(File storage) {

        if (storage == null) {
            return "Armazenamento";
        }


        StorageManager sm =
                (StorageManager) getSystemService(Context.STORAGE_SERVICE);


        if (sm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            for (StorageVolume vol : sm.getStorageVolumes()) {


                File dir = null;


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    dir = vol.getDirectory();
                }


                if (dir != null &&
                        dir.getAbsolutePath()
                                .equals(storage.getAbsolutePath())) {


                    if (vol.isPrimary()) {
                        return "Armazenamento Interno";
                    }


                    String descricao =
                            vol.getDescription(this);


                    if (descricao != null &&
                            !descricao.trim().isEmpty()) {

                        return descricao;
                    }


                    if (vol.isRemovable()) {
                        return "Armazenamento Removível";
                    }
                }
            }
        }


        return "Armazenamento";
    }

    public String getStorageId2(File storage) {

        if (storage == null) {
            return "Armazenamento";
        }


        String caminho =
                storage.getAbsolutePath();


        File raiz =
                new File(caminho);


        String nome =
                raiz.getName();


        // Memória interna
        if (caminho.contains("emulated")
                || nome.equals("0")) {

            return "Interno";
        }


        // Exemplo: 7A3B-19F2
        return nome;
    }

    private java.util.ArrayList<File> listaVideos;

    private boolean isSeekToCalled = false;
    private String currentPlayingPath = null;

    private StyledPlayerView playerViewFundo;
    private ExoPlayer player;
    //private PlaylistFragment fragmentNavegacao;
    boolean isPlayerAtivo = false;

    // Dados de Navegação
    private final List<MenuSection> sections = new ArrayList<>();
    private int currentIndex = 0;
    private int lastSectionIndex = -1;
    private int lastTabIndex = -1;
    private int currentTab = 0;
    private int savedVideoCodecInt; // Variável global
    private final int seasonTabWidth = 306;
    private final int seasonTabHeight = 48;
    private boolean isFocusOnTabs = false; // controla se o foco está na linha de abas
    private long lastMoveTime = 0; // debounce do foco
    private boolean isMenuVisible = false;
    private boolean isNewFolderNavigation = false;
    private int lastFocusedIndex = 0; // Adicione isso lá em cima junto com as outras variáveis
    private String lastPath = "";

    private final List<String> displayItems = new ArrayList<>();
    private final List<String> listaIdiomas = new ArrayList<>();

    private FrameLayout containerPlaylist;
    private int lastFocusedMenuIndex = 0; // guarda o índice do item focado no menu

    public enum SectionType {
        PLAYLIST, CONFIGURATIONS
    }

    // SETUP DE SEÇÕES
    @SuppressLint("NewApi")
    private void setupSections() {

        sections.clear();

        // ===============================
        // PLAYLIST
        // ===============================


        // 📦 adiciona seção
        sections.add(new MenuSection(
                SectionType.PLAYLIST,
                "PLAYLIST",
                null
        ));


        sections.add(new MenuSection(
                SectionType.CONFIGURATIONS,
                "CONFIGURAÇÕES DE PLAYER",
                Arrays.asList("RETORNAR","OPENING","ENDING","TEMAS")
        ));
    }

    public static class MenuSection {
        public SectionType type;
        public String title;
        public List<String> items;
        public MenuSection(SectionType type, String title, List<String> items) {
            this.type = type; this.title = title; this.items = items;
        }
    }

    //
    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void listaVerticalSubir(View v, View v2) {
        if (v == null) return;


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

    private void alterarAlturaPlaylist(boolean ativo) {

        if (containerPlaylist == null)
            return;

        ViewGroup.LayoutParams params =
                containerPlaylist.getLayoutParams();

        params.height = ativo ? dp(166) : 0;

        containerPlaylist.setLayoutParams(params);

        containerPlaylist.setVisibility(
                ativo ? View.VISIBLE : View.GONE
        );
    }

    // MENU - CONFIGURAÇÕES
    @SuppressLint("NewApi")
    private void updateConfiguracoes() {
        MenuSection current = sections.get(currentIndex);

        // limpa logo
        // LIMPA MENU ATUAL
        menu_actvContent.removeAllViews();
        displayItems.clear();


        // =====================================================
        // CONTROLE DO FRAGMENT PLAYLIST
        // =====================================================

        if (current.type == SectionType.PLAYLIST) {

            alterarAlturaPlaylist(true);


        } else {

            alterarAlturaPlaylist(false);

        }

        if (currentIndex != lastSectionIndex) {

            // Determina direÃ§Ã£o da animaÃ§Ã£o: +80f para baixo, −80f para cima
            float dirY = (currentIndex > lastSectionIndex && lastSectionIndex != -1) ? 80f : -80f;

            menuContainer.animate().cancel(); // cancela animações pendentes
            menuContainer.setAlpha(0f);
            menuContainer.setTranslationY(dirY);

            menuContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .start();

            lastSectionIndex = currentIndex;
            lastTabIndex = currentTab; // Sincroniza para a próxima troca de aba ser "limpa"
        } else if (currentTab != lastTabIndex) {
            // Apenas troca de aba: sem animação
            lastTabIndex = currentTab;
        }

        // CENTER (PROTEGIDO)
        if (menu_pvCenter != null) {

            menu_pvCenter.setText(current.title);
            menu_pvCenter.setTextColor(ThemeManager.SEM_100);

            menu_pvCenter.setShadowLayer(
                    8f,
                    0f,
                    4f,
                    ThemeManager.COM_100
            );
        }

        menu_actvContent.setOrientation(LinearLayout.VERTICAL);

        int itemWidth, itemHeight;
        switch (current.type) {
            case PLAYLIST:
            case CONFIGURATIONS:
            default:
                itemHeight = 72;
                break;
        }

        LinearLayout layoutEps = new LinearLayout(this);
        layoutEps.setOrientation(LinearLayout.HORIZONTAL);

        List<String> displayItems = new ArrayList<>();

        if (current.type == SectionType.PLAYLIST) {


        }

        else if (current.type == SectionType.CONFIGURATIONS) {

            // Página 3: opções reais
            switch (currentTab) {
                case 0:
                    displayItems.addAll(Arrays.asList("SIM", "NAO"));
                    break;

                case 1: // OPENING
                    // Se já foi configurado (≥ 1), mostra "OPENING DEFINED", senão "DEFINIR OPENING"
                    String txtOp = (prefsHelper.getEscolhaOpening() >= 1) ? "OPENING DEFINIDO" : "DEFINIR OPENING";
                    displayItems.addAll(Arrays.asList(txtOp, "NAO"));
                    break;

                case 2: // ENDING
                    // Se já foi configurado (≥ 1), mostra "ENDING DEFINED", senão "DEFINIR ENDING"
                    String txtEd = (prefsHelper.getEscolhaEndings() >= 1) ? "ENDING DEFINIDO" : "DEFINIR ENDING";
                    displayItems.addAll(Arrays.asList(txtEd, "NAO"));
                    break;

                case 3: // TEMAS
                    displayItems.addAll(Arrays.asList("PADRAO", "ANEMO", "GEO", "ELECTRO", "DENDRO", "HYDRO", "PYRO", "CRYO"));
                    break;
            }
        }

        else {
            // Página 2: RESOLUTION ou outras seções simples
            if (current.items != null) {
                displayItems.addAll(current.items);
            }
        }

        for (int i = 0; i < displayItems.size(); i++) {
            String label = displayItems.get(i);
            TextView tv = new TextView(this);


            final int tabIndex = i; // fixa o Ã­ndice para o listener

            tv.setText(label);
            tv.setTypeface(getResources().getFont(R.font.new_font));
            tv.setTypeface(null, Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            tv.setSingleLine(true);
            tv.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            tv.setMarqueeRepeatLimit(-1);
            tv.setHorizontallyScrolling(true);
            tv.setLetterSpacing(0.3f);
            tv.setTextSize(21);
            tv.setSelected(true);
            tv.setClickable(true);
            tv.setFocusable(true);
            tv.setFocusableInTouchMode(true);
            tv.setPadding(15,0, 15,0);

            // 1️⃣ Gera o ID
            tv.setId(View.generateViewId());

            // 2️⃣ Trava de foco para itens
            if (current.type == SectionType.PLAYLIST || current.type == SectionType.CONFIGURATIONS) {

                tv.setNextFocusDownId(tv.getId());
                tv.setNextFocusUpId(tv.getId());

                if (i == 0)
                    tv.setNextFocusLeftId(tv.getId());

                if (i == displayItems.size() - 1)
                    tv.setNextFocusRightId(tv.getId());
            }

            // ---------- LARGURA ----------
            switch (current.type) {
                case PLAYLIST:
                    itemWidth = dp(122);
                    break;
                case CONFIGURATIONS:
                    // ---------- LARGURA (CORRIGIDO) ----------
                    switch (currentTab) {
                        case 0: // RETORNAR
                            if (label.equals("SIM") || label.equals("NAO")) {
                                itemWidth = dp(140);
                            } else {
                                itemWidth = dp(160);
                            }
                            break;

                        case 1: // OPENING
                            if (label.equals("DEFINIR OPENING") || label.equals("OPENING DEFINIDO")) {
                                itemWidth = dp(340);
                            } else {
                                itemWidth = dp(160);
                            }
                            break;

                        case 2: // ENDINGS
                            if (label.equals("DEFINIR ENDING") || label.equals("ENDING DEFINIDO")) {
                                itemWidth = dp(320);
                            } else {
                                itemWidth = dp(160);
                            }
                            break;

                        case 3: // TEMAS (Genshin Elements)
                            if ("PADRAO".equals(label)) itemWidth = dp(210);
                            else if ("ANEMO".equals(label)) itemWidth = dp(200);
                            else if ("GEO".equals(label)) itemWidth = dp(140);
                            else if ("ELECTRO".equals(label)) itemWidth = dp(230);
                            else if ("DENDRO".equals(label)) itemWidth = dp(210);
                            else if ("HYDRO".equals(label)) itemWidth = dp(210);
                            else if ("PYRO".equals(label)) itemWidth = dp(160);  // Adicionado largura para Pyro
                            else if ("CRYO".equals(label)) itemWidth = dp(160);  // Adicionado largura para Cryo
                            else itemWidth = dp(280);                            // Fallback de segurança para a aba 3
                            break; // ⚡ CORREÇÃO: Break adicionado para impedir o fall-through para o default global

                        default:
                            itemWidth = dp(280);
                            break;
                    }

                    break;
                default:
                    itemWidth = dp(280);
                    break;
            }

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(itemWidth, itemHeight);
            lp.setMargins(5, 0, 5, 0);
            tv.setLayoutParams(lp);

            int selectedIndex = prefsHelper.getLastThemePosition();

            int[] colorsTemas = {
                    ContextCompat.getColor(this, R.color.PADRAO_COM_100),
                    ContextCompat.getColor(this, R.color.ANEMO_COM_100),
                    ContextCompat.getColor(this, R.color.GEO_COM_100),
                    ContextCompat.getColor(this, R.color.ELECTRO_COM_100),
                    ContextCompat.getColor(this, R.color.DENDRO_COM_100),
                    ContextCompat.getColor(this, R.color.HYDRO_COM_100),
                    ContextCompat.getColor(this, R.color.PYRO_COM_100),
                    ContextCompat.getColor(this, R.color.CRYO_COM_100)
            };


            // ---------- CHECK ----------
            boolean isChecked = false;
            switch (current.type) {
                case PLAYLIST:

                    break;

                case CONFIGURATIONS:
                    switch (currentTab) {
                        case 0:
                            if ((label.equals("SIM") && prefsHelper.getEscolhaRetornar() == 1)
                                    || (label.equals("NAO") && prefsHelper.getEscolhaRetornar() == 0))
                                isChecked = true;
                            break;

                        case 1: // OPENING
                            // Marcador para o botão de configuração (Aceita DEFINIR ou DEFINED)
                            if ((label.equals("DEFINIR OPENING") || label.equals("OPENING DEFINIDO"))
                                    && prefsHelper.getEscolhaOpening() >= 1) {
                                isChecked = true;
                            }
                            // Marcador para o padrão NÃO (Modo 0)
                            else if (label.equals("NAO") && prefsHelper.getEscolhaOpening() == 0) {
                                isChecked = true;
                            }
                            break;

                        case 2: // ENDING
                            // Marcador para o botão de configuração (Aceita DEFINIR ou DEFINED)
                            if ((label.equals("DEFINIR ENDING") || label.equals("ENDING DEFINIDO"))
                                    && prefsHelper.getEscolhaEndings() >= 1) {
                                isChecked = true;
                            }
                            // Marcador para o padrão NÃO (Modo 0)
                            else if (label.equals("NAO") && prefsHelper.getEscolhaEndings() == 0) {
                                isChecked = true;
                            }
                            break;
                        case 3:
                            if ((label.equals("PADRAO") && selectedIndex == 0)
                                    || (label.equals("ANEMO") && selectedIndex == 1)
                                    || (label.equals("GEO") && selectedIndex == 2)
                                    || (label.equals("ELECTRO") && selectedIndex == 3)
                                    || (label.equals("DENDRO") && selectedIndex == 4)
                                    || (label.equals("HYDRO") && selectedIndex == 5)
                                    || (label.equals("PYRO") && selectedIndex == 6)
                                    || (label.equals("CRYO") && selectedIndex == 7))
                                isChecked = true;
                            break;
                    }
                    break;
            }

// ---------- FOCO INICIAL ----------
            boolean shouldFocus = (isChecked && !isFocusOnTabs);

// ---------- CORES ----------
            if (shouldFocus) {
                tv.setBackgroundColor(ThemeManager.COM_100);
                tv.setTextColor(ThemeManager.BLACK_100);
                tv.requestFocus();
            } else {
                tv.setBackgroundColor(isChecked ? ThemeManager.COM_080 : ThemeManager.SEM_100);
                tv.setTextColor(isChecked ? ThemeManager.BLACK_080 : ThemeManager.COM_100);
            }

// ---------- CORES (Renderização Inicial) ----------
            if (shouldFocus) {

                tv.setBackgroundColor(ThemeManager.COM_100);
                tv.setTextColor(ThemeManager.BLACK_100);

                if (current.type == SectionType.CONFIGURATIONS && currentTab == 3) {
                    int corDoItemDesteTema = colorsTemas[tabIndex];

                    // âœ… APLICA CONTRASTE NO INÃCIO TAMBÃ‰M: Se cor do item == tema atual, fica PRETO
                    if (corDoItemDesteTema == ThemeManager.COM_100) {
                        tv.setTextColor(ThemeManager.BLACK_100);
                    } else {
                        tv.setTextColor(corDoItemDesteTema);
                    }
                } else {
                    tv.setTextColor(ThemeManager.BLACK_100);
                }
                tv.post(tv::requestFocus);
            } else {

                tv.setBackgroundColor(isChecked ? ThemeManager.COM_080 : ThemeManager.SEM_100);
                tv.setTextColor(isChecked ? ThemeManager.BLACK_080 : ThemeManager.COM_100);

            }

            // 2. ---------- FOCO DINÂMICO (Interação do Usuário) ----------
            boolean finalIsChecked = isChecked;
            tv.setOnFocusChangeListener((v, hasFocus) -> {

                tv.setSelected(hasFocus); // necessário para o marquee funcionar

                if (hasFocus) {
                    tv.setBackgroundColor(ThemeManager.COM_100);
                    tv.setTextColor(ThemeManager.BLACK_100);
                    if (current.type == SectionType.CONFIGURATIONS && currentTab == 3) {
                        int corDoItemDesteTema = colorsTemas[tabIndex];

                        // âœ… CONTRASTE NA NAVEGAÃ‡ÃƒO: Se cor do item == tema atual, fica PRETO
                        if (corDoItemDesteTema == ThemeManager.COM_100) {
                            tv.setTextColor(ThemeManager.BLACK_100);
                        } else {
                            tv.setTextColor(corDoItemDesteTema);
                        }
                    } else {
                        tv.setTextColor(ThemeManager.BLACK_100);
                    }
                    //centerItemInCarousel(findViewById(R.id.hScroll), v);
                } else {
                    // Comportamento normal ao sair do foco (Volta para cores apagadas)
                    boolean checkedNow = finalIsChecked;

                    tv.setBackgroundColor(checkedNow ? ThemeManager.COM_080 : ThemeManager.SEM_100);
                    tv.setTextColor(checkedNow ? ThemeManager.BLACK_080 : ThemeManager.COM_100);
                }
            });
            // CLIQUE NO ITEM (PASTA OU VÍDEO)
            // ---------- CLIQUE ----------
            tv.setOnClickListener(v -> {
                switch (current.type) {
                    case PLAYLIST:

                        break;

                    case CONFIGURATIONS:
                        switch (currentTab) {
                            case 3: // TEMAS
                                // 1. Descobre a posiÃ§Ã£o (0 a 7)
                                String[] nomesTemas = {"PADRAO", "ANEMO", "GEO", "ELECTRO", "DENDRO", "HYDRO", "PYRO", "CRYO"};
                                int themeIndex = 0;
                                for (int j = 0; j < nomesTemas.length; j++) {
                                    if (label.equals(nomesTemas[j])) {
                                        themeIndex = j;
                                        break;
                                    }
                                }

                                // 2. SALVA E APLICA (Aqui o COM_100 global muda)
                                prefsHelper.setLastThemePosition(themeIndex);

                                // 1. Aplica o tema salvando e atualizando a memória estática na hora
                                ThemeManager manager = new ThemeManager();
                                manager.apply(C04_VideosGridActivity.this, themeIndex);

                                // 2. Força o seu fragmento Leanback a recarregar as cores e manter a posição salva do foco
                                if (playlistFragment != null) {
                                    playlistFragment.forcarRecarregamentoDasCores2();
                                }

                                // 3. âœ… CORRIGINDO O "SUMIÃ‡O" (Contraste no ato do clique)
                                // Pegamos a cor que acabamos de definir como tema do sistema
                                int novaCorSistema = colorsTemas[themeIndex];
                                int corDesteItem = colorsTemas[themeIndex];

                                // ForÃ§amos o fundo a brilhar com a nova cor
                                tv.setBackgroundColor(novaCorSistema);

                                // Se a cor do item clicado for a mesma que a nova cor do sistema, fica PRETO
                                if (corDesteItem == novaCorSistema) {
                                    tv.setTextColor(ThemeManager.BLACK_100); // ðŸ‘ˆ Isso impede que fique "cor sobre cor"
                                } else {
                                    tv.setTextColor(corDesteItem);
                                }

                                // 4. ATUALIZA O RESTO DA UI
                                updateConfiguracoes();
                                break;
                        }
                        break;

                }
                updateConfiguracoes();
            });

            layoutEps.addView(tv);
        }

        menu_actvContent.addView(layoutEps);

        LinearLayout layoutTabs = findViewById(R.id.layoutTabs);
        layoutTabs.removeAllViews();
        layoutTabs.setPadding(5, 15, 0, 0); // desceu 10dp

        if (current.type == SectionType.CONFIGURATIONS) {

            // CRIAR AS ABAS DE CONFIGURAÇÃO (RETORNAR, ENDINGS, AUDIO, CODIFICADOR)
            LinearLayout layout_configuracoes = new LinearLayout(this);
            layout_configuracoes.setOrientation(LinearLayout.HORIZONTAL);
            layout_configuracoes.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

            for (int i = 0; i < current.items.size(); i++) {
                String labelAba = current.items.get(i);
                final int tabIdx = i;

                TextView mnConfiguracoes = new TextView(this);
                mnConfiguracoes.setText(labelAba);
                mnConfiguracoes.setGravity(Gravity.CENTER);
                mnConfiguracoes.setTypeface(getResources().getFont(R.font.new_font));
                mnConfiguracoes.setTextSize(14);
                mnConfiguracoes.setFocusable(true);
                mnConfiguracoes.setId(View.generateViewId());

                // TRAVAS DE EIXO
                mnConfiguracoes.setNextFocusUpId(mnConfiguracoes.getId());
                mnConfiguracoes.setNextFocusDownId(mnConfiguracoes.getId());

                // TRAVAS LATERAIS
                if (i == 0) mnConfiguracoes.setNextFocusLeftId(mnConfiguracoes.getId());
                if (i == current.items.size() - 1) mnConfiguracoes.setNextFocusRightId(mnConfiguracoes.getId());

                mnConfiguracoes.setPadding(15, 10, 15, 10);
                mnConfiguracoes.setLayoutParams(new LinearLayout.LayoutParams(160, seasonTabHeight));


                // Estilo da Aba
                if (tabIdx == currentTab) {
                    if (isFocusOnTabs) {
                        mnConfiguracoes.setBackgroundColor(ThemeManager.COM_100);
                        mnConfiguracoes.setTextColor(ThemeManager.BLACK_100);
                        mnConfiguracoes.post(mnConfiguracoes::requestFocus);
                    } else {
                        mnConfiguracoes.setTextColor(ThemeManager.COM_100);
                        mnConfiguracoes.setBackgroundColor(Color.TRANSPARENT);
                    }
                } else {
                    mnConfiguracoes.setTextColor(ThemeManager.SEM_080);
                    mnConfiguracoes.setBackgroundColor(Color.TRANSPARENT);
                }

                mnConfiguracoes.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        mnConfiguracoes.setBackgroundColor(ThemeManager.COM_100);
                        mnConfiguracoes.setTextColor(ThemeManager.BLACK_100);
                        if (currentTab != tabIdx) {
                            currentTab = tabIdx;
                            updateConfiguracoes();
                        }
                    } else {
                        mnConfiguracoes.setBackgroundColor(Color.TRANSPARENT);
                        mnConfiguracoes.setTextColor(tabIdx == currentTab ? ThemeManager.COM_100 : ThemeManager.SEM_080);
                    }
                });

                layout_configuracoes.addView(mnConfiguracoes);
            }

            layoutTabs.addView(layout_configuracoes);
        }

        updatePreviews(); // atualiza miniaturas ou previews da seção

    }


    // VERIFICA SE TEM TABS
    private boolean sectionHasTabs(MenuSection section) {
        if (section == null) return false; // 🔥 proteção
        return section.type == SectionType.CONFIGURATIONS;
    }

    // PREVIEWS DE MENU
    private void updatePreviews() {

        if (sections == null || sections.isEmpty()) return;

        // 🔥 proteção extra
        if (currentIndex < 0 || currentIndex >= sections.size()) return;

        // =========================
        // TOP (anterior)
        // =========================
        if (menu_pvTop != null) {

            if (currentIndex > 0) {

                MenuSection prev = sections.get(currentIndex - 1);

                if (prev != null) {
                    menu_pvTop.setText(prev.title);
                }

                  menu_pvTop.setVisibility(View.VISIBLE);
               menu_pvTop.setAlpha(0.25f);

            } else {

                menu_pvTop.setVisibility(View.GONE);
                menu_pvTop.setAlpha(1f);
               menu_pvTop.setShadowLayer(0f, 0f, 0f, 0);
            }
        }

        // =========================
        // BOTTOM (próximo)
        // =========================
        if (menu_pvBottom != null) {

            if (currentIndex < sections.size() - 1) {

                MenuSection next = sections.get(currentIndex + 1);

                if (next != null) {
                    menu_pvBottom.setText(next.title);
                }

                menu_pvBottom.setVisibility(View.VISIBLE);
                menu_pvBottom.setAlpha(0.5f);

            } else {

                menu_pvBottom.setVisibility(View.INVISIBLE);
                menu_pvBottom.setAlpha(1f);
                menu_pvBottom.setShadowLayer(0f, 0f, 0f, 0);
            }
        }}

    public void atualizarTituloAtivo2(String t) { if (menu_pvTop != null) menu_pvTop.setText(t); }
    public void atualizarCaminhoBottom2(CharSequence c) {
        if (menu_pvBottom != null) {
            menu_pvBottom.setText(c);
            menu_pvBottom.setTextColor(ThemeManager.SEM_100);
        }
    }



    @Override
    protected void onStart() {
        super.onStart();
        // Se a tela voltar e o player estiver ativo mas pausado, retoma o vídeo no fundo
        if (player != null && isPlayerAtivo && !player.isPlaying()) {
            player.play();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Força o estado padrão do timer ao retomar o foco da Activity na TV
        isSeekingTimerRunning = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (player != null && isPlayerAtivo) {
            if (player.isPlaying()) {
                // Pausa a reprodução. Isso dispara automaticamente o onIsPlayingChanged(false),
                // que executa todo o salvamento seguro e sincronizado no seu PrefsHelper!
                player.pause();
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Desativa a flag global do player
        isPlayerAtivo = false;

        // Liberação obrigatória e total de hardware do ExoPlayer na TV Box
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    @SuppressLint("RestrictedApi")
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (isBloqueador) return true;
        if (event == null) return false;

        int keyCode = event.getKeyCode();
        int action = event.getAction();
        long now = System.currentTimeMillis();

        Log.e("EndKEY", "Menu Visible: " + keyCode);

        // Evita repetição muito rápida (apenas para ACTION_DOWN)
        if (action == KeyEvent.ACTION_DOWN) {
            if (now - lastMoveTime < 100) return true;
            lastMoveTime = now;
        }


        View explorerRoot = findViewById(R.id.explorer_root);
        boolean isMenuVisivel = (explorerRoot != null && explorerRoot.getVisibility() == View.VISIBLE);

        // ==========================================
        // TECLA MENU (CÓDIGO 82) ALTERNA A INTERFACE
        // ==========================================
        if (keyCode == KeyEvent.KEYCODE_MENU && action == KeyEvent.ACTION_DOWN) {
            if (explorerRoot != null) {
                if (!isMenuVisivel) {
                    isPlayerAtivo = false;
                    if (explorerBackground != null) explorerBackground.setVisibility(View.VISIBLE);
                    listaVerticalSubir(explorerRoot, null);
                    layoutControlesPlayer.setVisibility(View.GONE);
                } else {
                    isPlayerAtivo = (player != null && player.isPlaying());
                    if (isPlayerAtivo) {
                        if (explorerBackground != null) explorerBackground.setVisibility(View.GONE);
                        layoutControlesPlayer.setVisibility(View.VISIBLE);
                    }
                }
                return true;
            }
        }

            // Tecla Voltar: Se temas estiver aberto, apenas esconde ele primeiro
            if (keyCode == KeyEvent.KEYCODE_BACK && action == KeyEvent.ACTION_DOWN) {
                // 1. Tenta voltar um nível na navegação de pastas da playlist
                if (playlistFragment != null && playlistFragment.voltarParaPastaAnterior2()) {
                    return true;
                }

                // 2. Se o player estiver ativo/reproduzindo, esconde o menu e volta para o player
                if (player != null && player.isPlaying()) {
                    isPlayerAtivo = true;

                    // Oculta o fundo absoluto do menu
                    if (explorerBackground != null) {
                        explorerBackground.setVisibility(View.GONE);
                    }

                    // CORREÇÃO: Esconde o menu com animação e restaura os controles do player
                    listaVerticalDescer2(explorerRoot, null, null);
                    layoutControlesPlayer.setVisibility(View.VISIBLE);

                    return true;
                }
        }


        // =======================
            // DPAD DOWN
            // =======================
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && action == KeyEvent.ACTION_DOWN) {

                if (sections == null || sections.isEmpty()) return true;

                MenuSection current = sections.get(currentIndex);

                // 1️⃣ Move foco para abas
                if (sectionHasTabs(current) && !isFocusOnTabs) {
                    if (current.items != null && !current.items.isEmpty()) {
                        isFocusOnTabs = true;
                        currentTab = Math.min(currentTab, current.items.size() - 1);
                        updateConfiguracoes();
                        return true;
                    }
                }

                // 2️⃣ Próxima seção
                if (currentIndex < sections.size() - 1) {
                    currentIndex++;
                    MenuSection next = sections.get(currentIndex);

                    currentTab = 0;
                    isFocusOnTabs = sectionHasTabs(next) && next.items != null && !next.items.isEmpty();

                    updateConfiguracoes();
                    return true;
                }

                return false;
            }

            // =======================
            // DPAD UP
            // =======================
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && action == KeyEvent.ACTION_DOWN) {

                if (sections == null || sections.isEmpty()) return true;

                MenuSection current = sections.get(currentIndex);

                // 1️⃣ Sai das abas
                if (sectionHasTabs(current) && isFocusOnTabs) {
                    isFocusOnTabs = false;
                    updateConfiguracoes();
                    return true;
                }

                // 2️⃣ Seção anterior
                if (currentIndex > 0) {

                    currentIndex--;

                    MenuSection prev = sections.get(currentIndex);

                    currentTab = 0;
                    isFocusOnTabs = sectionHasTabs(prev)
                            && prev.items != null
                            && !prev.items.isEmpty();

                    updateConfiguracoes();

                    // cheguei aqui ele procura o caminho salvo no getLastStoragePath() e coloca o foco nele
                    if (currentIndex == 0) {

                        if (containerPlaylist != null) {
                            containerPlaylist.postDelayed(() -> {
                                if (playlistFragment != null && playlistFragment.getView() != null) {
                                    playlistFragment.getView().requestFocus();
                                }
                            }, 60);
                        }
                    }

                    return true;
                }

                return false;
            }

            // =======================
            // DPAD CENTER / OK
            // =======================
            if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                    && action == KeyEvent.ACTION_DOWN) {

                View focusedView = getCurrentFocus();

                if (focusedView != null) {
                    focusedView.performClick();
                }

                return true;
            }


        return super.dispatchKeyEvent(event);
    }




    public void listaVerticalDescer2(View v, View v2, Runnable fin) {
        isBloqueador = true;
        if (v == null || v.getVisibility() != View.VISIBLE) { if (fin != null) fin.run(); isBloqueador = false; return; }
        v.animate().cancel();
        v.animate().alpha(0f).translationY(150f).setDuration(BACKGROUND_FADE_IN_MS)
                .setInterpolator(new android.view.animation.AccelerateInterpolator()).withLayer()
                .withEndAction(() -> {
                    v.setVisibility(View.GONE); v.setTranslationY(0f); v.setAlpha(1f);
                    if (fin != null) v.postDelayed(() -> { fin.run(); isBloqueador = false; }, 30L);
                    else isBloqueador = false;
                }).start();
    }

    public void actualizarImagemDeFundo2(Object modeloMidia) {
        if (explorerBackground == null) return;

        // REGRA DA TECLA 1: Se o wallpaper foi DESATIVADO, limpa tudo e some com a parede.
        if (!isWallpaperAtivo) {
            com.bumptech.glide.Glide.with(this).clear(explorerBackground);
            explorerBackground.setBackground(null);
            explorerBackground.setVisibility(View.GONE);
            return;
        }

        // Se o menu foi ocultado (assistindo em tela cheia), bloqueia também
        if (isPlayerAtivo) return;

        // Garante que a view está visível
        explorerBackground.setVisibility(View.VISIBLE);

        Log.d("FOCO", "actualizarImagemDeFundo2 -> " + modeloMidia);

        // OTIMIZAÇÃO DE CACHING: Se for um recurso do APK (Integer), não precisamos de cache em disco pesado
        com.bumptech.glide.load.engine.DiskCacheStrategy estrategiaCache =
                (modeloMidia instanceof Integer)
                        ? com.bumptech.glide.load.engine.DiskCacheStrategy.NONE
                        : com.bumptech.glide.load.engine.DiskCacheStrategy.ALL;

        com.bumptech.glide.Glide.with(this.getApplicationContext()) // CORREÇÃO 1: Use o contexto da aplicação no CustomTarget para evitar Memory Leaks
                .load(modeloMidia)
                .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(250))
                .diskCacheStrategy(estrategiaCache) // CORREÇÃO 2: Cache inteligente para não engasgar trocando de temas
                .centerCrop()
                .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull android.graphics.drawable.Drawable resource,
                                                @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.drawable.Drawable> transition) {
                        // Só aplica se o usuário não tiver mudado de ideia nesse meio tempo
                        if (isWallpaperAtivo && !isPlayerAtivo && explorerBackground != null) {
                            explorerBackground.setBackground(resource);
                        }
                    }
                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                        if (explorerBackground != null) {
                            explorerBackground.setBackground(null);
                        }
                    }
                });
    }

    public void limparImagemDeFundo2() {
        if (explorerBackground == null) return;

        // Se o wallpaper estiver desativado pela tecla 1, mantém escondido pro vídeo passar limpo
        if (!isWallpaperAtivo) {
            explorerBackground.setVisibility(View.GONE);
            return;
        }

        if (!isPlayerAtivo) {
            int resIdPadrao = getResources().getIdentifier("padrao_thumb", "mipmap", getPackageName());
            if (resIdPadrao == 0) resIdPadrao = getResources().getIdentifier("padrao_thumb", "drawable", getPackageName());

            if (resIdPadrao != 0) {
                carregarFundoComGlide2(resIdPadrao);
            } else {
                com.bumptech.glide.Glide.with(this).clear(explorerBackground);
                explorerBackground.setBackground(null);
                explorerBackground.setBackgroundColor(Color.parseColor("#111111"));
            }
        }
    }

    private void carregarFundoComGlide2(Object modeloImagem) {


        Log.d("FOCO", "carregarFundoComGlide2 -> " + modeloImagem);

        com.bumptech.glide.Glide.with(this)
                .load(modeloImagem)
                .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(250))
                .centerCrop()
                .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull android.graphics.drawable.Drawable resource, @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.drawable.Drawable> transition) {
                        if (explorerBackground != null && !isPlayerAtivo && isWallpaperAtivo) {
                            explorerBackground.setVisibility(View.VISIBLE);
                            explorerBackground.setBackground(resource);
                        }
                    }
                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
                });
    }


    public void reproduzirPlaylistNoFundo2(java.util.ArrayList<File> listaVideos, int indexInicial) {
        if (listaVideos == null || listaVideos.isEmpty()) return;
        this.listaVideos = listaVideos; // Guarda a referência globalmente para o onPause e loops

        // MARCA O ESTADO DO PLAYER COMO ATIVO E SOME COM A VIEW DA IMAGEM IMEDIATAMENTE
        isPlayerAtivo = true;
        if (explorerBackground != null) {
            com.bumptech.glide.Glide.with(this).clear(explorerBackground);
            explorerBackground.setBackground(null);
            explorerBackground.setVisibility(View.GONE);
        }

        if (player != null) {
            // Antes de liberar o player antigo, garante o salvamento do vídeo que estava rodando
            salvarProgressoDoVideoAtual();
            player.release();
        }

        player = new ExoPlayer.Builder(this).build();
        playerViewFundo.setPlayer(player);

        // Monta a playlist nativa do ExoPlayer com base estrita no que veio da tela
        java.util.List<com.google.android.exoplayer2.MediaItem> exoplayerPlaylist = new java.util.ArrayList<>();
        for (File video : listaVideos) {
            exoplayerPlaylist.add(com.google.android.exoplayer2.MediaItem.fromUri(android.net.Uri.fromFile(video)));
        }

        player.setMediaItems(exoplayerPlaylist);

        // RECUPERAÇÃO DE HISTÓRICO: Verifica se o vídeo inicial clicado já possui tempo salvo no banco
        long progressoSalvo = 0;
        if (indexInicial >= 0 && indexInicial < listaVideos.size() && prefsHelper != null) {
            File videoInicial = listaVideos.get(indexInicial);
            String folderName = videoInicial.getParentFile() != null ? videoInicial.getParentFile().getName() : "Root";
            String cleanTitle = videoInicial.getName().replace(".mp4", "").replace(".MP4", "");
            String key = folderName + "/" + cleanTitle + ".mp4";

            // Só faz o seek se o status não for COMPLETO (para não recomeçar do fim)
            if (!PrefsHelper.STATUS_COMPLETE.equals(prefsHelper.getLastVideoStatus(key))) {
                progressoSalvo = prefsHelper.getLastVideoProgress(key);
            }
        }

        // Salta direto para o vídeo clicado e aplica o segundo exato onde parou
        player.seekTo(indexInicial, progressoSalvo > 0 ? progressoSalvo : 0);

        player.prepare();
        player.setPlayWhenReady(true);

        layoutControlesPlayer.setVisibility(View.VISIBLE);
        txtIndicadorPause.setVisibility(View.GONE);

        player.addListener(new com.google.android.exoplayer2.Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (player == null) return;

                // =========================================================================
                // 1. 🎬 VÍDEO PRONTO PARA TOCAR (Retomar de onde parou)
                // =========================================================================
                if (state == com.google.android.exoplayer2.Player.STATE_READY && !isSeekToCalled) {

                    long dur = player.getDuration();
                    // C.TIME_UNSET equivale a -9223372036854775807L no ExoPlayer
                    if (dur <= 0 || dur == -9223372036854775807L) return;

                    // Gera a CHAVE PADRONIZADA (Pasta/Video.mp4) direto do arquivo puro
                    String keyComPasta = null;
                    if (listaVideos != null && position >= 0 && position < listaVideos.size()) {
                        File file = listaVideos.get(position);
                        String folderName = file.getParentFile() != null ? file.getParentFile().getName() : "Root";
                        String cleanTitle = file.getName().replace(".mp4", "").replace(".MP4", "");
                        keyComPasta = folderName + "/" + cleanTitle + ".mp4";
                    }

                    if (keyComPasta != null && prefsHelper != null) {
                        long saved = prefsHelper.getLastVideoProgress(keyComPasta);
                        int escolha = prefsHelper.getEscolhaRetornar();

                        // Só retoma se tiver mais de 10s e não estiver no final (faltando 10s)
                        boolean hasValidProgress = saved > 10_000 && saved < dur - 10_000;

                        if (hasValidProgress) {
                            if (escolha == 1) { // 1 = Sim (Retomar)
                                player.seekTo(saved);
                            } else {
                                player.seekTo(0);
                            }
                        }
                    }

                    player.play();
                    isSeekToCalled = true;
                }

                // =========================================================================
                // 2. ✅ VÍDEO TERMINOU (Marcar como Completo)
                // =========================================================================
                if (state == com.google.android.exoplayer2.Player.STATE_ENDED) {

                    long duration = player.getDuration();
                    if (duration <= 0 || duration == -9223372036854775807L) duration = 0;

                    int indexConcluido = position; // Armazena o índice do que acabou de terminar

                    if (listaVideos != null && indexConcluido >= 0 && indexConcluido < listaVideos.size() && prefsHelper != null) {

                        // Gera a CHAVE PADRONIZADA
                        File file = listaVideos.get(indexConcluido);
                        String folderName = file.getParentFile() != null ? file.getParentFile().getName() : "Root";
                        String cleanTitle = file.getName().replace(".mp4", "").replace(".MP4", "");
                        String keyComPasta = folderName + "/" + cleanTitle + ".mp4";

                        // UI Updates dos seus controles
                        //if (exo_seekbar != null) exo_seekbar.setProgress(1000);
                        //if (exo_int != null) exo_int.setText(convertTimer(duration));

                        // Salva como COMPLETO usando a chave de pasta
                        prefsHelper.setLastVideoStatus(keyComPasta, PrefsHelper.STATUS_COMPLETE);
                        prefsHelper.setLastVideoProgress(keyComPasta, duration);

                        Log.d("SALVAMENTO", "✅ Vídeo Concluído (Chave): " + keyComPasta);

                        // =========================================================================
                        // FORÇA ATUALIZAÇÃO VISUAL NA GRADE DE CARDS (FRAGMENT SEPARADO)
                        // =========================================================================
                        // Avisa o fragmentNavegacao para pintar o card antigo como 100% Completo
                        if (playlistFragment != null) {
                            // Passamos o mesmo índice duas vezes porque apenas o concluído mudou de estado visual
                            playlistFragment.notificarMudancaDeVideoNaGrade(indexConcluido, indexConcluido);
                        }
                        // =========================================================================
                    }

                    // 🚀 LÓGICA DE PRÓXIMO VÍDEO
                    if (player.hasNextMediaItem()) {
                        //isSeekToCalled = false;
                        player.seekToNext();
                        // O ExoPlayer atualiza o ponteiro interno e chama o onMediaItemTransition automaticamente
                        player.play();
                    } else {
                        // Fim da Playlist: Limpeza dos Handlers e fecha a tela
                        //if (myCurrentInterval != null) myCurrentInterval.removeCallbacks(seekingTimer);
                        //isSeekingTimerRunning = false;

                        player.stop();
                        //finish();
                        //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    }
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                // Só executa o salvamento se o player parou de tocar (isPlaying == false)
                if (!isPlaying && player != null && isPlayerAtivo) {

                    mLastPosition = player.getCurrentPosition();
                    long duration = player.getDuration();

                    if (duration < 0 || duration == -9223372036854775807L) duration = 0;

                    // Recupera o índice correto do vídeo que está em execução no fundo
                    int indexAtual = player.getCurrentMediaItemIndex();

                    if (listaVideos != null && indexAtual >= 0 && indexAtual < listaVideos.size() && prefsHelper != null && mLastPosition > 0 && duration > 0) {

                        File file = listaVideos.get(indexAtual);
                        String nomeDoVideo = file.getName();

                        // 1. GERA A CHAVE PADRONIZADA (Pasta/Video.mp4) DIRETO DO FILE
                        String folderName = file.getParentFile() != null ? file.getParentFile().getName() : "Root";
                        String cleanTitle = nomeDoVideo.replace(".mp4", "").replace(".MP4", "");
                        String keyComPasta = folderName + "/" + cleanTitle + ".mp4";

                        // 2. SALVAMENTO DE PROGRESSO
                        prefsHelper.setLastVideoProgress(keyComPasta, mLastPosition);
                        prefsHelper.setLastVideoDuration(keyComPasta, duration);

                        Log.d("SALVAMENTO", "onIsPlayingChanged | Key: " + keyComPasta + " | Pos: " + mLastPosition);

                        // 3. LÓGICA PADRÃO: ABRIU = PROGRESSO (Com suporte à regra dos 95% para TV)
                        String statusAtual = prefsHelper.getLastVideoStatus(keyComPasta);

                        if (mLastPosition >= (duration * 0.95)) {
                            prefsHelper.setLastVideoStatus(keyComPasta, PrefsHelper.STATUS_COMPLETE);
                            Log.d("SALVAMENTO", "Status: " + PrefsHelper.STATUS_COMPLETE);
                        } else if (!PrefsHelper.STATUS_COMPLETE.equals(statusAtual)) {
                            prefsHelper.setLastVideoStatus(keyComPasta, PrefsHelper.STATUS_IN_PROGRESS);
                            Log.d("SALVAMENTO", "Status: " + PrefsHelper.STATUS_IN_PROGRESS);
                        }

                        // Histórico e metadados globais da Activity sincronizados direto do File
                        prefsHelper.setLastVideoTitle(nomeDoVideo);
                        //prefsHelper.setLastVideoPosition(indexAtual);
                        //prefsHelper.setLastVideoImage(file.getPath());
                        //prefsHelper.setFromPlayer(true);

                        // 4. ATUALIZAÇÃO VISUAL DA GRADE DE CARDS (FRAGMENT SEPARADO)
                        if (playlistFragment != null) {
                            playlistFragment.notificarMudancaDeVideoNaGrade(indexAtual, indexAtual);
                        }
                    }
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable com.google.android.exoplayer2.MediaItem mediaItem, int reason) {
                // PROTEÇÃO: Garante que o player e a lista de vídeos existam
                if (player == null || listaVideos == null || listaVideos.isEmpty()) return;

                // Guarda a posição antiga antes de atualizar o ponteiro global
                final int positionAnterior = position;

                // =========================================================================
                // 1. SALVAR ESTADO DO VÍDEO QUE ESTÁ SAINDO
                // =========================================================================
                if (prefsHelper != null && positionAnterior >= 0 && positionAnterior < listaVideos.size()) {

                    File oldFile = listaVideos.get(positionAnterior);
                    String oldFolder = oldFile.getParentFile() != null ? oldFile.getParentFile().getName() : "Root";

                    // CORREÇÃO: Extrai o nome direto do arquivo antigo, sem depender de variáveis de UI
                    String oldFileName = oldFile.getName();
                    String oldCleanTitle = oldFileName.replace(".mp4", "").replace(".MP4", "");
                    String keyAnterior = oldFolder + "/" + oldCleanTitle + ".mp4";

                    long videoDuration = player.getDuration();
                    if (videoDuration <= 0 || videoDuration == -9223372036854775807L) videoDuration = 0;

                    if (reason == com.google.android.exoplayer2.Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        // Se mudou sozinho, o anterior completou
                        prefsHelper.setLastVideoStatus(keyAnterior, PrefsHelper.STATUS_COMPLETE);
                        prefsHelper.setLastVideoProgress(keyAnterior, videoDuration);
                        Log.d("SALVAMENTO", "Auto-Transição: " + keyAnterior + " marcado como COMPLETO");

                    } else if (reason == com.google.android.exoplayer2.Player.MEDIA_ITEM_TRANSITION_REASON_SEEK ||
                            reason == com.google.android.exoplayer2.Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {

                        // Se pulou manualmente, checa se estava no final (98%)
                        long currentPos = player.getCurrentPosition();
                        float percent = (videoDuration > 0) ? (currentPos * 100f / videoDuration) : 0f;

                        if (percent >= 98f) {
                            prefsHelper.setLastVideoStatus(keyAnterior, PrefsHelper.STATUS_COMPLETE);
                            prefsHelper.setLastVideoProgress(keyAnterior, videoDuration);
                            Log.d("SALVAMENTO", "Manual-Transição (98%): " + keyAnterior + " marcado como COMPLETO");
                        } else if (currentPos > 0 && videoDuration > 0) {
                            // Salva o progresso parcial exato se pulou no meio
                            prefsHelper.setLastVideoStatus(keyAnterior, PrefsHelper.STATUS_IN_PROGRESS);
                            prefsHelper.setLastVideoProgress(keyAnterior, currentPos);
                            prefsHelper.setLastVideoDuration(keyAnterior, videoDuration);
                            Log.d("SALVAMENTO", "Manual-Transição (Parcial): " + keyAnterior + " salvo em " + percent + "%");
                        }
                    }
                }

                // =========================================================================
                // 2. ATUALIZAR PARA O NOVO ÍNDICE
                // =========================================================================
                final int newPosition = player.getCurrentMediaItemIndex();
                if (newPosition < 0 || newPosition >= listaVideos.size()) return;

                position = newPosition; // Atualiza o ponteiro numérico global

                // =========================================================================
                // 3. ATUALIZA METADADOS DO NOVO VÍDEO DIRETO DO FILE
                // =========================================================================
                File currentVideo = listaVideos.get(position);

                if (prefsHelper != null) {
                    //prefsHelper.setDisabledOpenings(false);
                    //prefsHelper.setDisabledEndings(false);
                    //prefsHelper.setLastVideo(position);
                    //prefsHelper.setLastVideoName(currentVideo.getName()); // Usa o nome direto do File
                    //prefsHelper.setLastVideoPath(currentVideo.getPath());
                    //prefsHelper.setFromPlayer(true);
                }

                if (mediaItem != null && mediaItem.localConfiguration != null) {
                    currentPlayingPath = mediaItem.localConfiguration.uri.getPath();
                }

                // =========================================================================
                // 4. ATUALIZAR UI E NOTIFICAR GRADE SEPARADA
                // =========================================================================
                //updateTitleUI(position);

                // Envia os comandos de atualização para o Fragment que gerencia o rowsAdapter
                if (playlistFragment != null) {
                    playlistFragment.notificarMudancaDeVideoNaGrade(positionAnterior, newPosition);
                }

                Log.d("mLogs", "Novo vídeo iniciado: " + currentVideo.getName() + " na pasta: " +
                        (currentVideo.getParentFile() != null ? currentVideo.getParentFile().getName() : "Root"));
            }

        });
    }

    private void salvarProgressoDoVideoAtual() {
        if (player == null || listaVideos == null || prefsHelper == null) return;

        int indexAtual = player.getCurrentMediaItemIndex();
        if (indexAtual >= 0 && indexAtual < listaVideos.size()) {
            File file = listaVideos.get(indexAtual);
            long currentPos = player.getCurrentPosition();
            long duration = player.getDuration();

            if (duration > 0 && currentPos > 0) {
                String folderName = file.getParentFile() != null ? file.getParentFile().getName() : "Root";
                String cleanTitle = file.getName().replace(".mp4", "").replace(".MP4", "");
                String keyComPasta = folderName + "/" + cleanTitle + ".mp4";

                prefsHelper.setLastVideoProgress(keyComPasta, currentPos);
                prefsHelper.setLastVideoDuration(keyComPasta, duration);

                String statusAtual = prefsHelper.getLastVideoStatus(keyComPasta);
                if (currentPos >= (duration * 0.95)) {
                    prefsHelper.setLastVideoStatus(keyComPasta, PrefsHelper.STATUS_COMPLETE);
                } else if (!PrefsHelper.STATUS_COMPLETE.equals(statusAtual)) {
                    prefsHelper.setLastVideoStatus(keyComPasta, PrefsHelper.STATUS_IN_PROGRESS);
                }

                prefsHelper.setLastVideoTitle(file.getName());
                //prefsHelper.setLastVideoPosition(indexAtual);
                //prefsHelper.setLastVideoImage(file.getPath());
                //prefsHelper.setFromPlayer(true);
            }
        }
    }
    private int position = 0;
    private long mLastPosition = 0;

    private boolean isSeekingTimerRunning = false;

}
