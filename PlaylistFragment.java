package my.endsousa.tv;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

import my.endsousa.tv.theme.ThemeManager;


public class PlaylistFragment extends RowsSupportFragment {


    private ArrayObjectAdapter rowsAdapter;
    private ArrayObjectAdapter itensAdapter;

    public static final String SET_MAIN = "MAIN";

    private final Stack<File> historicoDeNavegacao = new Stack<>();
    private ItemPresenter navegadorPresenter;
    private File pastaRaizAtual = null;

    private PrefsHelper prefsHelper;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefsHelper = new PrefsHelper(getContext());

        // Configura o seletor apenas para a linha de arquivos/pastas
        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();

        ListRowPresenter listRowPresenter = new ListRowPresenter();
        listRowPresenter.setHeaderPresenter(null);

        // Vincula apenas a ListRow padrão
        presenterSelector.addClassPresenter(ListRow.class, listRowPresenter);

        rowsAdapter = new ArrayObjectAdapter(presenterSelector);
        setAdapter(rowsAdapter);

        navegadorPresenter = new ItemPresenter();

        // Carrega apenas os dispositivos/pastas
        carregarNivelDispositivos();

        configurarMonitorDeFoco2();

        // O clique agora lida apenas com Pastas e Arquivos (Removido o bloco String de temas)
        // O clique lida perfeitamente com Pastas e Arquivos no novo ecossistema C04
        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
            if (item instanceof File) {
                final File selecionado = (File) item;

                if (selecionado.isDirectory()) {
                    if (pastaRaizAtual != null) {
                        historicoDeNavegacao.push(pastaRaizAtual);
                    }
                    pastaRaizAtual = selecionado;
                    atualizarTelaComConteudoDaPasta(selecionado);
                } else {
                    // CORREÇÃO 1: Cast atualizado para a sua nova classe C04_VideosGridActivity
                    if (getActivity() instanceof C04_VideosGridActivity) {
                        final C04_VideosGridActivity activity = (C04_VideosGridActivity) getActivity();

                        // CORREÇÃO 2: ID atualizado para rastrear a nova raiz do menu (root_layout_down ou root_video_player)
                        View explorerRoot = activity.findViewById(R.id.root_layout_down);

                        // =========================================================================
                        // CAPTURA A PLAYLIST DA PASTA ABERTA NA TELA DA TV
                        // =========================================================================
                        java.util.ArrayList<File> listaDeVideosDaTela = new java.util.ArrayList<>();
                        int indexDoVideoClicado = 0;

                        if (rowsAdapter != null && rowsAdapter.size() > 0) {
                            androidx.leanback.widget.ListRow primeiraLinha = (androidx.leanback.widget.ListRow) rowsAdapter.get(0);
                            androidx.leanback.widget.ObjectAdapter adapterDaLinha = primeiraLinha.getAdapter();

                            int contador = 0;
                            for (int i = 0; i < adapterDaLinha.size(); i++) {
                                Object itemDaTela = adapterDaLinha.get(i);
                                if (itemDaTela instanceof File) {
                                    File arquivoDaTela = (File) itemDaTela;
                                    if (!arquivoDaTela.isDirectory()) {
                                        listaDeVideosDaTela.add(arquivoDaTela);

                                        if (arquivoDaTela.getAbsolutePath().equals(selecionado.getAbsolutePath())) {
                                            indexDoVideoClicado = contador;
                                        }
                                        contador++;
                                    }
                                }
                            }
                        }
                        // =========================================================================

                        final java.util.ArrayList<File> playlistFinal = listaDeVideosDaTela;
                        final int posicaoInicial = indexDoVideoClicado;

                        Runnable acaoIniciarPlaylist = () -> {
                            // Envia a lista exata da tela e o índice para a nova Activity
                            activity.reproduzirPlaylistNoFundo2(playlistFinal, posicaoInicial);
                        };

                        // CORREÇÃO 3: Executa a animação de descida usando o método atual da sua classe
                        if (explorerRoot != null) {
                            activity.listaVerticalDescer2(explorerRoot, null, acaoIniciarPlaylist);
                        } else {
                            acaoIniciarPlaylist.run();
                        }
                    }
                }
            }
        });

    }

    public void configurarMonitorDeFoco2() {
        setOnItemViewSelectedListener((itemViewHolder, item, rowViewHolder, row) -> {
            if (isAdded() && getActivity() instanceof C04_VideosGridActivity) {
                if (item instanceof File) {
                    File arquivoOuPastaFocada = (File) item;
                    // CORREÇÃO: Cast para a Activity correta
                    C04_VideosGridActivity activity = (C04_VideosGridActivity) getActivity();

                    // Usa o seu método original que retorna "Invocacao / filme.mp4"
                    String textoCompleto = obterCaminhoSimplificado(arquivoOuPastaFocada);
                    CharSequence caminhoFinalParaOBottom = textoCompleto;

                    if (!arquivoOuPastaFocada.isDirectory()) {
                        String nomeVideo = arquivoOuPastaFocada.getName();
                        SpannableString spannable = new SpannableString(textoCompleto);

                        int inicio = textoCompleto.lastIndexOf(nomeVideo);
                        int fim = inicio + nomeVideo.length();

                        if (inicio != -1) {
                            if (inicio > 0) {
                                spannable.setSpan(
                                        new ForegroundColorSpan(ThemeManager.SEM_100),
                                        0,
                                        inicio,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                );
                            }

                            spannable.setSpan(
                                    new ForegroundColorSpan(ThemeManager.SEM_100),
                                    inicio,
                                    fim,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            );

                            spannable.setSpan(
                                    new CharacterStyle() {
                                        @Override
                                        public void updateDrawState(TextPaint tp) {
                                            tp.setColor(ThemeManager.COM_100);
                                            tp.setShadowLayer(4.0f, 2.0f, 2.0f, ThemeManager.SEM_020);
                                        }
                                    },
                                    inicio,
                                    fim,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            );

                            caminhoFinalParaOBottom = spannable;
                        }
                    }

                    // Envia o texto para o rodapé
                    activity.atualizarCaminhoBottom2(caminhoFinalParaOBottom);

                    // =========================================================================
                    // 2. TROCA DINÂMICA DE ACORDO COM O FOCO (PASTA OU VÍDEO)
                    // =========================================================================
                    if (arquivoOuPastaFocada.isDirectory()) {
                        String nomePasta = arquivoOuPastaFocada.getName();
                        File imagemFundo = new File(arquivoOuPastaFocada, nomePasta + ".png");
                        if (!imagemFundo.exists()) {
                            imagemFundo = new File(arquivoOuPastaFocada, nomePasta + ".jpg");
                        }
                        if (!imagemFundo.exists()) {
                            imagemFundo = new File(arquivoOuPastaFocada, nomePasta + ".jpeg");
                        }

                        if (imagemFundo.exists()) {
                            activity.actualizarImagemDeFundo2(imagemFundo);
                        } else {
                            // CORREÇÃO: Em vez de limpar e deixar preto, carrega o papel de parede padrão do tema atual!
                            activity.actualizarImagemDeFundo2(ThemeManager.IMAGE);
                        }
                    } else {
                        activity.actualizarImagemDeFundo2(arquivoOuPastaFocada);
                    }
                }
            }
        });
    }

    public String obterCaminhoSimplificado(File arquivo) {
        if (arquivo == null || !(getActivity() instanceof C04_VideosGridActivity )) {
            return "";
        }

        C04_VideosGridActivity  activity = (C04_VideosGridActivity ) getActivity();
        ArrayList<File> armazenamentos = activity.getStorages2();

        boolean isVideo = !arquivo.isDirectory();
        File pastaAlvo = isVideo ? arquivo.getParentFile() : arquivo;

        if (pastaAlvo == null) {
            return "";
        }

        File dispositivoOrigem = null;
        String caminhoBrutoDaPasta = pastaAlvo.getAbsolutePath();
        int maiorRaiz = 0;

        // Encontra o armazenamento correto
        for (File storage : armazenamentos) {
            String caminhoStorage = storage.getAbsolutePath();
            if (caminhoBrutoDaPasta.startsWith(caminhoStorage) && caminhoStorage.length() > maiorRaiz) {
                dispositivoOrigem = storage;
                maiorRaiz = caminhoStorage.length();
            }
        }

        String nomeBaseDispositivo = dispositivoOrigem != null
                ? activity.getStorageId2(dispositivoOrigem)
                : "Armazenamento";

        String caminhoRelativo = "";
        if (dispositivoOrigem != null) {
            String raizPath = dispositivoOrigem.getAbsolutePath();
            if (caminhoBrutoDaPasta.length() > raizPath.length()) {
                caminhoRelativo = caminhoBrutoDaPasta.substring(raizPath.length());
            }
        } else {
            caminhoRelativo = caminhoBrutoDaPasta;
        }

        if (caminhoRelativo.startsWith("/")) {
            caminhoRelativo = caminhoRelativo.substring(1);
        }

        // =========================================================================
        // LÓGICA DE RETORNO ADAPTADA
        // =========================================================================
        if (caminhoRelativo.isEmpty()) {
            // Caso especial: o vídeo ou pasta está direto na RAIZ do dispositivo
            if (isVideo) {
                return nomeBaseDispositivo + " / " + arquivo.getName();
            }
            return nomeBaseDispositivo;
        }

        // Se chegou aqui, existe pelo menos uma pasta intermediária
        String[] partesDasPastas = caminhoRelativo.split("/");
        String ultimaPasta = partesDasPastas[partesDasPastas.length - 1];

        if (isVideo) {
            // CORREÇÃO: Oculte o dispositivo e mostre apenas "Pasta / Filme.mp4"
            return ultimaPasta + " / " + arquivo.getName();
        }

        // Se for apenas a pasta selecionada, mantém o padrão com o dispositivo (ex: sdcard / Invocacao)
        return nomeBaseDispositivo + " / " + ultimaPasta;
    }
    public void carregarNivelDispositivos() {
        rowsAdapter.clear();
        pastaRaizAtual = null;

        if (!(getActivity() instanceof C04_VideosGridActivity )) {
            return;
        }

        C04_VideosGridActivity  activity = (C04_VideosGridActivity ) getActivity();

        ArrayObjectAdapter dispositivosAdapter = new ArrayObjectAdapter(navegadorPresenter);
        ArrayList<File> armazenamentos = activity.getStorages2();

        for (File storage : armazenamentos) {
            dispositivosAdapter.add(storage);
        }

        if (dispositivosAdapter.size() > 0) {
            rowsAdapter.add(new ListRow(new HeaderItem(0, "Dispositivos"), dispositivosAdapter));
        }

        configurarMonitorDeFoco2();
        activity.limparImagemDeFundo2();
    }

    public void forcarRecarregamentoDasCores2() {
        if (rowsAdapter == null || rowsAdapter.size() == 0) return;

        // 1. CAPTURA A LINHA E A COLUNA ATUALIZADA DO CURSOR DA TV ANTES DA MUDANÇA
        int linhaSelecionada = getSelectedPosition();
        int itemSelecionado = 0;

        androidx.leanback.widget.VerticalGridView verticalGridView = getVerticalGridView();
        if (verticalGridView != null) {
            // Busca o container da linha atual usando a API pública estável
            androidx.recyclerview.widget.RecyclerView.ViewHolder rowHolder =
                    verticalGridView.findViewHolderForAdapterPosition(linhaSelecionada);

            if (rowHolder != null && rowHolder.itemView != null) {
                // Dentro do container da linha, localiza a lista horizontal real do Leanback
                androidx.leanback.widget.HorizontalGridView horizontalGridView =
                        rowHolder.itemView.findViewById(androidx.leanback.R.id.row_content);

                if (horizontalGridView != null) {
                    // Salva o índice horizontal da pasta/arquivo focado
                    itemSelecionado = horizontalGridView.getSelectedPosition();
                }
            }
        }

        // 2. Cria o novo adapter temporário idêntico para forçar o recarregamento das cores
        ArrayObjectAdapter novoAdapter = new ArrayObjectAdapter(rowsAdapter.getPresenterSelector());
        for (int i = 0; i < rowsAdapter.size(); i++) {
            novoAdapter.add(rowsAdapter.get(i));
        }

        rowsAdapter = novoAdapter;
        setAdapter(rowsAdapter);

        // 3. FAZ O FOCO RETORNAR EXATAMENTE PARA A MESMA PASTA/ARQUIVO
        final int finalLinha = linhaSelecionada;
        final int finalItem = itemSelecionado;

        if (verticalGridView != null) {
            verticalGridView.post(() -> {
                // Força o cursor visual a acender de volta no mesmo card horizontal
                setSelectedPosition(finalLinha, true,
                        new androidx.leanback.widget.ListRowPresenter.SelectItemViewHolderTask(finalItem));
            });
        }

        // 4. Recarrega o monitor de foco para ler as novas variáveis estáticas do Wallpaper
        configurarMonitorDeFoco2();
    }

    private void atualizarTelaComConteudoDaPasta(File pasta) {
        rowsAdapter.clear();

        // Remove temporariamente o listener para limpar o estado antigo
        setOnItemViewSelectedListener(null);

        ArrayObjectAdapter linhaUnicaMesclada = new ArrayObjectAdapter(navegadorPresenter);

        File[] arquivos = pasta.listFiles();
        if (arquivos != null) {

            // =========================================================================
            // INJEÇÃO DA ORDENAÇÃO ALFANUMÉRICA NATURAL ANTES DOS LOOPINGS
            // =========================================================================
            java.util.Arrays.sort(arquivos, (f1, f2) -> compareAlphanumeric(f1.getName(), f2.getName()));

            // Adiciona primeiro as subpastas (já ordenadas alfanumericamente)
            for (File arquivo : arquivos) {
                if (arquivo.isDirectory() && !arquivo.getName().startsWith(".")) {
                    if (possuiVideoNestaPasta(arquivo)) {
                        linhaUnicaMesclada.add(arquivo);
                    }
                }
            }
            // Adiciona depois os arquivos de vídeo na mesma linha (já ordenados alfanumericamente)
            for (File arquivo : arquivos) {
                if (arquivo.isFile() && isVideoFile(arquivo)) {
                    linhaUnicaMesclada.add(arquivo);
                }
            }
        }

        if (linhaUnicaMesclada.size() > 0) {
            rowsAdapter.add(new ListRow(new HeaderItem(0, pasta.getName()), linhaUnicaMesclada));
        }

        // ATUALIZAÇÃO DOS TEXTOS E REATIVAÇÃO DO MONITOR DE FOCO
        if (isAdded() && getActivity() instanceof C04_VideosGridActivity ) {
            C04_VideosGridActivity  activity = (C04_VideosGridActivity ) getActivity();

            // Corrigido o nome do método para atualizar o rodapé com o caminho simplificado (Ex: Cartão SD > Pasta)
            activity.atualizarCaminhoBottom2(obterCaminhoSimplificado(pasta));

            // CRÍTICO PARA TV: Ativa o monitor inteligente novamente para as subpastas/arquivos responderem ao foco
            configurarMonitorDeFoco2();
        }
    }

    // ==========================================
    // 🔢 ORDENAÇÃO ALFANUMÉRICA NATURAL (OTIMIZADA)
    // ==========================================
    private int compareAlphanumeric(String s1, String s2) {
        if (s1 == null) s1 = "";
        if (s2 == null) s2 = "";

        // O Regex divide o texto em blocos separados de letras e números puramente
        String[] parts1 = s1.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        String[] parts2 = s2.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

        int minLen = Math.min(parts1.length, parts2.length);
        for (int i = 0; i < minLen; i++) {
            String p1 = parts1[i];
            String p2 = parts2[i];

            // Verifica de forma leve se ambas as partes são numéricas
            boolean isDigit1 = Character.isDigit(p1.charAt(0));
            boolean isDigit2 = Character.isDigit(p2.charAt(0));

            if (isDigit1 && isDigit2) {
                // Remove zeros à esquerda para a comparação não falhar (ex: "02" vs "2")
                String cleanP1 = p1.replaceFirst("^0+", "");
                String cleanP2 = p2.replaceFirst("^0+", "");
                if (cleanP1.isEmpty()) cleanP1 = "0";
                if (cleanP2.isEmpty()) cleanP2 = "0";

                // PROTEÇÃO CONTRA NÚMEROS GIGANTES: Se os comprimentos forem diferentes, o maior número vence
                if (cleanP1.length() != cleanP2.length()) {
                    return Integer.compare(cleanP1.length(), cleanP2.length());
                }

                // Se tiverem o mesmo tamanho, uma comparação de texto comum resolve a ordem numérica perfeitamente
                int cmp = cleanP1.compareTo(cleanP2);
                if (cmp != 0) return cmp;

            } else {
                // Comparação comum de texto ignorando maiúsculas e minúsculas
                int result = p1.compareToIgnoreCase(p2);
                if (result != 0) return result;
            }
        }

        // Se empatar em tudo até aqui, a string com mais blocos remanescentes vem depois
        return Integer.compare(parts1.length, parts2.length);
    }

    private boolean possuiVideoNestaPasta(File pasta) {
        if (pasta == null || !pasta.exists()) return false;
        File[] arquivosDaPasta = pasta.listFiles();
        if (arquivosDaPasta == null) return false;

        for (File arquivo : arquivosDaPasta) {
            if (arquivo.isFile()) {
                // Reaproveita o método isVideoFile unificado para melhor manutenção do código
                if (isVideoFile(arquivo)) {
                    return true;
                }
            } else if (arquivo.isDirectory() && !arquivo.getName().startsWith(".")) {
                if (possuiVideoNestaPasta(arquivo)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean voltarParaPastaAnterior2() {
        if (!historicoDeNavegacao.isEmpty()) {
            File pastaAnterior = historicoDeNavegacao.pop();
            pastaRaizAtual = pastaAnterior;
            atualizarTelaComConteudoDaPasta(pastaAnterior);
            return true;
        } else if (pastaRaizAtual != null) {
            carregarNivelDispositivos();
            // CRÍTICO PARA TV: Ativa o monitor de foco centralizado ao retornar para a raiz de dispositivos
            configurarMonitorDeFoco2();
            return true;
        }
        return false;
    }

    // DENTRO DO SEU FRAGMENTO (Onde fica o rowsAdapter e o onViewCreated)
    public void notificarMudancaDeVideoNaGrade(int indexAnterior, int indexAtual) {
        if (rowsAdapter != null && rowsAdapter.size() > 0) {
            // Roda na UI Thread de forma segura
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Acessa a primeira linha de conteúdo do Leanback
                    Object primeiraLinha = rowsAdapter.get(0);
                    if (primeiraLinha instanceof ListRow) {
                        Object adapterInterno = ((ListRow) primeiraLinha).getAdapter();
                        if (adapterInterno instanceof ArrayObjectAdapter) {
                            ArrayObjectAdapter cardAdapter = (ArrayObjectAdapter) adapterInterno;

                            // 1. Atualiza o card do vídeo que saiu para aplicar o status COMPLETO
                            if (indexAnterior >= 0 && indexAnterior < cardAdapter.size()) {
                                cardAdapter.notifyArrayItemRangeChanged(indexAnterior, 1);
                            }

                            // 2. Atualiza o card do novo vídeo que começou a tocar agora
                            if (indexAtual >= 0 && indexAtual < cardAdapter.size()) {
                                cardAdapter.notifyArrayItemRangeChanged(indexAtual, 1);
                            }
                        }
                    }
                }
            });
        }
    }

    private boolean isVideoFile(File file) {
        if (file == null) return false;
        String name = file.getName().toLowerCase();
        // Lista expandida com extensões modernas e populares para Android TV
        return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi")
                || name.endsWith(".ts") || name.endsWith(".webm") || name.endsWith(".mov")
                || name.endsWith(".3gp");
    }

}
