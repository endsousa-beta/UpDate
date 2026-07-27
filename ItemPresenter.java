package my.endsousa.tv;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.card.MaterialCardView;
import com.makeramen.roundedimageview.RoundedImageView;
import java.io.File;
import java.util.ArrayList;

import my.endsousa.tv.theme.ThemeManager;

public class ItemPresenter extends Presenter {

    private HorizontalScrollView scrollView;
    private PrefsHelper prefsHelper;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        // CORREÇÃO CRÍTICA CONTRA CRASH: Força o inflador a usar o wrapper do Material Components
        // Isso impede o erro de ThemeEnforcement na TV, mesmo que o tema da activity seja Leanback.
        Context contextWrapper = new ContextThemeWrapper(parent.getContext(),
                com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar);

        View view = LayoutInflater.from(contextWrapper).inflate(R.layout.main_items, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        File arquivoOuPasta = (File) item;
        boolean isFolder = arquivoOuPasta.isDirectory();
        View folderView = viewHolder.view;

        // =========================================================================
        // CORREÇÃO CRÍTICA DE CONTEXTO: Resgata o contexto real da árvore de Views
        // =========================================================================
        Context context = folderView.getContext();
        ThemeManager themeManager = new ThemeManager();
        themeManager.load(context); // Substituído 'activity' por 'context' de forma segura

        // CORREÇÃO DE PERFORMANCE: Inicialize o PrefsHelper apenas se for nulo
        if (prefsHelper == null) {
            // Em Presenters, use o contexto da própria view para evitar vazamento de memória
            prefsHelper = new PrefsHelper(context);
        }

        // CORREÇÃO: Vincula a variável correta (arquivoOuPasta) diretamente à View que recebe o foco (folderView)
        folderView.setTag(arquivoOuPasta);

        LinearLayout row = folderView.findViewById(R.id.rowItem);
        LinearLayout card = folderView.findViewById(R.id.cardView);
        RoundedImageView thumb = folderView.findViewById(R.id.cardImage);
        LinearLayout cardUpdown = folderView.findViewById(R.id.mini_updown);

        FrameLayout cardShadow = folderView.findViewById(R.id.cardSombra);
        FrameLayout pgsView = folderView.findViewById(R.id.mini_view);

        ProgressBar progress = folderView.findViewById(R.id.mini_pgsb);
        ProgressBar pgsLoad = folderView.findViewById(R.id.mini_load);


        // =========================================================================
        // APLICAÇÃO DINÂMICA DO TEMA NOS CARDS DE DISPOSITIVOS/PASTAS
        // =========================================================================
        if (cardUpdown != null) {
            cardUpdown.getLayoutParams().height = 0;
            // Aplica a cor de destaque principal do tema atualizado
            cardUpdown.setBackgroundColor(ThemeManager.COM_040);
            cardUpdown.requestLayout();
        }

        if (card != null) {
            // Define a cor de fundo do Card dinamicamente de acordo com o tema selecionado
            card.setBackgroundColor(ThemeManager.SEM_100);
        }

        if (cardShadow != null) {
            // Define a cor da sombra/borda de acordo com as especificações do tema
            cardShadow.setBackgroundColor(ThemeManager.SEM_020);
        }

        //String status = prefsHelper.getLastVideoStatus(arquivoOuPasta.getName() + ".mp4");
        //boolean isComplete = PrefsHelper.STATUS_COMPLETE.equals(status);

        pgsLoad.setIndeterminateTintList(ColorStateList.valueOf(ThemeManager.COM_100));
        pgsLoad.setBackground(ContextCompat.getDrawable(context, ThemeManager.ICON));
        pgsView.setBackground(ContextCompat.getDrawable(context, ThemeManager.THUMBR));

        if (pgsView.getBackground() != null)
            pgsView.getBackground().setAlpha(51);

        pgsView.setVisibility(View.VISIBLE);

// 1. TRATAMENTO DE DISPOSITIVO (Ajuste conforme sua lógica de negócio)
        // 1. TRATAMENTO DE DISPOSITIVO (Ajuste conforme sua lógica de negócio)
        if (isDispositivo(arquivoOuPasta)) {
            progress.setVisibility(View.GONE);
            pgsLoad.setVisibility(View.VISIBLE);

            // Exemplo de reset e carregamento para Dispositivo
            if (thumb != null) thumb.setColorFilter(null);
            if (card != null) card.setBackgroundColor(ThemeManager.SEM_100);

            Glide.with(thumb.getContext()) // CORREÇÃO 1: Use o contexto direto do ImageView alvo
                    .load(R.mipmap.electro_icon)
                    // CORREÇÃO 2: Remova o '.transform(new CenterCrop())' para logotipos/ícones
                    // O CenterCrop corta as bordas e quebra o carregamento de mipmaps/vetores
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            pgsView.setVisibility(View.GONE);
                            // DICA: Coloque um breakpoint aqui ou use Log.e("GLIDE", "Erro:", e); para ler o erro real
                            return false;
                        }
                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            pgsView.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .error(ThemeManager.THUMBR)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(thumb);
        }

else {

// 2. CONFIGURAÇÃO VISUAL
            if (isFolder) {
                // 2. TRATAMENTO DE PASTA
                progress.setVisibility(View.GONE);
                pgsLoad.setVisibility(View.VISIBLE);

                // RESET RADICAL PARA PASTAS: Limpa heranças de reciclagem de cards de vídeos assistidos
                if (thumb != null) thumb.setColorFilter(null);
                if (card != null) card.setBackgroundColor(ThemeManager.SEM_100);

                // Busca otimizada pelas extensões de imagem
                String[] extensoes = {".png", ".jpg", ".jpeg"};
                File seasonImage = null;

                for (String ext : extensoes) {
                    File teste = new File(arquivoOuPasta, arquivoOuPasta.getName() + ext);
                    if (teste.exists()) {
                        seasonImage = teste;
                        break;
                    }
                }

                // Se achou a imagem, usa ela. Se não, passa o diretório (cairá no .error)
                Object localCarregamento = (seasonImage != null) ? seasonImage : ThemeManager.IMAGE;

                Glide.with(folderView.getContext())
                        .load(localCarregamento)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .transform(new CenterCrop()) // Simplificado, não precisa de 'new RequestOptions()'
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                pgsView.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                pgsView.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .error(ThemeManager.THUMBR)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // Cache apenas da imagem final processada
                        .into(thumb);

            } else {
                // 3. TRATAMENTO DE VÍDEO
                pgsLoad.setVisibility(View.VISIBLE);

                // O bindVideo gerencia de forma centralizada os estados de 'card', 'thumb' e 'progress'
                bindVideo(card, progress, thumb, arquivoOuPasta);

                // 3. Carrega a miniatura real do filme de forma assíncrona
                Glide.with(folderView.getContext())
                        .asBitmap() // Força o Glide a decodificar o vídeo como um frame estático (Bitmap)
                        .load(arquivoOuPasta)
                        .transition(BitmapTransitionOptions.withCrossFade())
                        .centerCrop() // OTIMIZAÇÃO: Usa o método nativo direto do Glide em vez de instanciar o CenterCrop
                        .listener(new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                pgsView.setVisibility(View.GONE);
                                // REMOVIDO: Não mexa no 'progress' aqui para não anular o estado do bindVideo
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                pgsView.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .error(ThemeManager.THUMBR)
                        .diskCacheStrategy(DiskCacheStrategy.NONE) // Evita cache pesado de arquivos de vídeo locais
                        .priority(Priority.NORMAL)
                        .thumbnail(0.85f)
                        .into(thumb);
            }


        }

        // 3. EFEITO DE FOCO DO CONTROLE REMOTO DA TV CALIBRADO CORRETAMENTE
        folderView.setOnFocusChangeListener((v, hasFocus) -> {

            // BUSCA UNIFICADA NO PREFS: Executa apenas uma leitura para economizar memória e CPU da TV
            String fName = arquivoOuPasta.getParentFile() != null ? arquivoOuPasta.getParentFile().getName() : "Root";
            String cleanT = arquivoOuPasta.getName().replace(".mp4", "").replace(".MP4", "");
            String itemKey = fName + "/" + cleanT + ".mp4";

            Log.d("PREFS int",itemKey);

            String currentStatus = prefsHelper.getLastVideoStatus(itemKey);
            boolean itemIsComplete = !isFolder && PrefsHelper.STATUS_COMPLETE.equals(currentStatus);

            // Calibração de altura da animação
            int targetDip = isFolder ? 0 : (hasFocus ? 10 : 0);
            int badgeTargetHeight = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, targetDip, v.getResources().getDisplayMetrics());

            cardUpdown.animate().cancel();
            int currentHeight = cardUpdown.getHeight();

            if (currentHeight != badgeTargetHeight) {
                ValueAnimator badgeAnim = ValueAnimator.ofInt(currentHeight, badgeTargetHeight);
                badgeAnim.setDuration(200);
                badgeAnim.addUpdateListener(a -> {
                    if (cardUpdown.getLayoutParams() != null) {
                        cardUpdown.getLayoutParams().height = (int) a.getAnimatedValue();
                        cardUpdown.requestLayout();
                    }
                });
                badgeAnim.start();
            }

            if (hasFocus) {
                // =========================================================================
                // 🎬 QUANDO O ITEM GANHA O FOCO DO CONTROLE REMOTO
                // =========================================================================
                // CORREÇÃO CRÍTICA: Usa setCardBackgroundColor para manter as bordas arredondadas do MaterialCardView
                if (card != null) {
                    card.setBackgroundColor(ThemeManager.COM_100);
                }

                if (cardUpdown != null) {
                    cardUpdown.setBackgroundColor(itemIsComplete ? Color.argb(120, 0, 0, 0) : ThemeManager.COM_020);
                }

                if (thumb != null) {
                    thumb.setColorFilter(null); // Remove filtros escuros para acender a thumbnail focada
                }

                // Centralização Horizontal Fina na TV (Mantém o item no meio exato da linha)
                if (scrollView != null) {
                    int cardLeft = v.getLeft();
                    int cardWidth = v.getWidth();
                    int scrollWidth = scrollView.getWidth();
                    int x = cardLeft - (scrollWidth - cardWidth) / 2;
                    scrollView.smoothScrollTo(Math.max(x, 0), 0);
                }

                if (progress != null) {
                    progress.setProgressTintList(ColorStateList.valueOf(ThemeManager.SEM_100));
                    if (!isFolder) progress.setVisibility(View.VISIBLE);
                }

                // =========================================================================
                // NOVO: ENVIA O TEXTO E O WALLPAPER PARA A ACTIVITY
                // =========================================================================

                if (context instanceof C04_VideosGridActivity) {

                    Log.d("FOCO", "Contexto é VideosGridActivity");

                    C04_VideosGridActivity activity = (C04_VideosGridActivity) context;

                    Log.d("FOCO", "Context: " + context.getClass().getName());
                    Log.d("FOCO", "Tag: " + v.getTag());

                    if (v.getTag() instanceof File) {

                        Log.d("FOCO", "Tag é File");
                        File focado = (File) v.getTag();
                        Log.d("FOCO", "Arquivo: " + focado.getAbsolutePath());

                        String nomeParaOTopo;
                        ArrayList<File> raizes = activity.getStorages2();
                        boolean isDispositivo = raizes.contains(focado);

                        if (focado.isFile()) {
                            nomeParaOTopo = focado.getName();
                            Log.d("FOCO", "É arquivo: " + nomeParaOTopo);
                        } else {
                            if (isDispositivo) {
                                nomeParaOTopo = activity.getStorageType2(focado);
                                Log.d("FOCO", "É armazenamento: " + nomeParaOTopo);
                            } else {
                                nomeParaOTopo = focado.getName();
                                Log.d("FOCO", "É pasta: " + nomeParaOTopo);
                            }
                        }

                        // =========================================================================
                        // CORREÇÃO: ENVIO DO WALLPAPER DE ACORDO COM O TIPO DE ITEM FOCADO
                        // =========================================================================
                        if (isDispositivo) {
                            Log.d("FOCO", "Wallpaper: Carregando logo do dispositivo");
                            // CORREÇÃO: Usa o logo dinâmico do tema atualizado em vez de deixar fixado em pryo_logo
                            activity.actualizarImagemDeFundo2(ThemeManager.LOGO);

                        } else if (focado.isDirectory()) {
                            // Busca dinâmica por pôster dentro da pasta focada
                            String nomePasta = focado.getName();
                            File imagemFundo = null;
                            String[] extensoes = {".png", ".jpg", ".jpeg"};

                            for (String ext : extensoes) {
                                File teste = new File(focado, nomePasta + ext);
                                if (teste.exists()) {
                                    imagemFundo = teste;
                                    break;
                                }
                            }

                            if (imagemFundo != null) {
                                Log.d("FOCO", "Wallpaper: Encontrado pôster da pasta -> " + imagemFundo.getName());
                                activity.actualizarImagemDeFundo2(imagemFundo);
                            } else {
                                // =========================================================================
                                // EM VEZ DE PASSAR O INTEIRO PURO NO MÉTODO DE FILE, CHAME O METODO QUE
                                // TRATA RECURSOS DO ANDROID (MIPMAP/DRAWABLE) OU SEU FALLBACK DE IMAGEM
                                // =========================================================================
                                Log.d("FOCO", "Wallpaper: Pasta sem imagem, usando padrão do tema ativo");

                                // Se o seu método actualizarImagemDeFundo aceitar apenas File, você deve usar:
                                activity.actualizarImagemDeFundo2(ThemeManager.IMAGE);
                                // Nota: Certifique-se de que na VideosGridActivity o método accepta 'int' (sobrecarregado)
                            }
                        } else {
                            // Se for um arquivo de vídeo, manda o File para o Glide na Activity gerar o frame
                            Log.d("FOCO", "Wallpaper: Enviando arquivo de vídeo para extração de frame");
                            activity.actualizarImagemDeFundo2(focado);
                        }

                    }
                }


            } else {
                // =========================================================================
                // 🕒 QUANDO O ITEM PERDE O FOCO DO CONTROLE REMOTO (FINALIZAÇÃO DO SEU CÓDIGO)
                // =========================================================================
                if (card != null) {
                    if (itemIsComplete) {
                        // Restaura o visual escurecido se for um vídeo já concluído
                        card.setBackgroundColor(Color.argb(120, 0, 0, 0));
                    } else {
                        // Restaura para a cor padrão neutra do tema selecionado
                        card.setBackgroundColor(ThemeManager.SEM_100);
                    }
                }

                if (thumb != null) {
                    if (itemIsComplete) {
                        thumb.setColorFilter(Color.argb(120, 0, 0, 0));
                    } else {
                        thumb.setColorFilter(null);
                    }
                }

                if (cardUpdown != null) {
                    cardUpdown.setBackgroundColor(ThemeManager.SEM_020);
                }

                // O seu código foi interrompido aqui na leitura de progresso residual
                if (!isFolder && progress != null) {
                    long p = prefsHelper.getLastVideoProgress(itemKey);

                    // Se o item não tem progresso salvo, esconde a barra ao perder o foco para manter o grid limpo
                    if (p <= 0 || itemIsComplete) {
                        progress.setVisibility(View.GONE);
                    } else {
                        // Se possui progresso ativo, restaura a cor secundária neutra da barra
                        progress.setProgressTintList(ColorStateList.valueOf(ThemeManager.SEM_080));
                    }
                }
            }
        });

    }

    private void bindVideo(LinearLayout card, ProgressBar progress, ImageView thumb, File arquivoVideo) {

        // 1. RESET RADICAL (Essencial para não herdar cores de itens anteriores no scroll)
        if (thumb != null) thumb.setColorFilter(null);
        if (card != null) card.setBackgroundColor(ThemeManager.SEM_100);
        progress.setVisibility(View.GONE);
        progress.setProgress(0);

        if (arquivoVideo == null) {
            if (card != null) card.setBackgroundColor(Color.TRANSPARENT);
            return;
        }

        if (prefsHelper == null) {
            prefsHelper = new PrefsHelper(progress.getContext());
        }

        // 2. NOVA CHAVE SEGURA (Pasta/Arquivo.mp4) - SINCRONIZADA COM O PLAYER
        String folderName = arquivoVideo.getParentFile() != null ? arquivoVideo.getParentFile().getName() : "Root";

        // Limpa o nome para evitar o erro de ".mp4.mp4" e bater com a chave do onPause
        String cleanTitle = arquivoVideo.getName().replace(".mp4", "").replace(".MP4", "");
        String key = folderName + "/" + cleanTitle + ".mp4";

        Log.d("PREFS out", key);

        // 3. RECUPERAÇÃO DE DADOS
        String status = prefsHelper.getLastVideoStatus(key);
        long p = prefsHelper.getLastVideoProgress(key);
        long d = prefsHelper.getLastVideoDuration(key);

        boolean isComplete = PrefsHelper.STATUS_COMPLETE.equals(status);
        boolean hasProgress = p > 0 && !isComplete;

        // 4. LÓGICA DE INTERFACE VISUAL (SEM TEXTOS)
        if (isComplete) {
            // VÍDEO CONCLUÍDO
            progress.setVisibility(View.VISIBLE);
            progress.setProgress(100);
            progress.setProgressTintList(ColorStateList.valueOf(ThemeManager.COM_020));

            // Escurece o card e a thumb para dar o efeito de "assistido"
            if (card != null) card.setBackgroundColor(Color.argb(120, 0, 0, 0));
            if (thumb != null) thumb.setColorFilter(Color.argb(120, 0, 0, 0));

            Log.d("bindVideo", "✅ COMPLETO: " + key);

        } else if (hasProgress) {
            // VÍDEO EM ANDAMENTO
            int percent = (d > 0) ? (int) ((p * 100L) / d) : 0;
            percent = Math.max(1, Math.min(99, percent));

            progress.setVisibility(View.VISIBLE);
            progress.setProgress(percent);
            progress.setProgressTintList(ColorStateList.valueOf(ThemeManager.SEM_080));

            Log.d("bindVideo", "⏳ PROGRESSO: " + key + " (" + percent + "%)");

        } else {
            // VÍDEO NOVO
            Log.d("bindVideo", "🆕 NOVO: " + key);
        }
    }


    private boolean isDispositivo(File arquivo) {
        if (arquivo == null) {
            return false;
        }

        String caminho = arquivo.getAbsolutePath();

        // 1. Caminho padrão do armazenamento interno (/storage/emulated/0)
        String armazenamentoInterno = Environment.getExternalStorageDirectory().getAbsolutePath();

        // 2. Se for exatamente a raiz interna ou se for a raiz direta de '/storage' ou '/storage/emulated'
        if (caminho.equals(armazenamentoInterno) ||
                caminho.equals("/storage") ||
                caminho.equals("/storage/emulated")) {
            return true;
        }

        // 3. Validação para cartões SD externos ou Pendrives (/storage/XXXX-XXXX)
        // Uma raiz de dispositivo externo geralmente é "/storage/XXXX-XXXX", sem subpastas adicionais
        File parent = arquivo.getParentFile();
        if (parent != null && parent.getAbsolutePath().equals("/storage")) {
            // Se o pai é "/storage" e ele é um diretório, significa que é a raiz de algum armazenamento
            return arquivo.isDirectory() && !arquivo.getName().equals("emulated");
        }

        return false;
    }


    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        // Captura o RoundedImageView do seu XML de forma limpa e interrompe o Glide
        RoundedImageView cardImage = viewHolder.view.findViewById(R.id.cardImage);
        if (cardImage != null) {
            Glide.with(viewHolder.view.getContext()).clear(cardImage);
        }
    }
}
