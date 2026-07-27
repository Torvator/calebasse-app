package fr.calebassecalvasse.app;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.WebViewListener;

public class MainActivity extends BridgeActivity {

    // Bleu de la charte (identique à notification_accent dans res/values/colors.xml).
    private static final int BLEU_CHARTE = 0xFF2447D6;
    // Course à faire avec le doigt pour armer le rafraîchissement. Plus long
    // que le défaut : le geste doit être intentionnel, pas déclenché en
    // effleurant une liste vers le bas.
    private static final int COURSE_DP = 140;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        installerTirerPourActualiser();
    }

    /**
     * Enveloppe la WebView Capacitor dans un SwipeRefreshLayout : balayer
     * vers le bas depuis le haut de page recharge le site. Le geste vit côté
     * natif (et non dans le site) pour rester disponible même quand la page
     * distante est cassée ou restée bloquée — c'est justement son rôle.
     */
    private void installerTirerPourActualiser() {
        // Capacitor renonce à créer le bridge quand la WebView système est
        // indisponible (mise à jour en cours, composant désactivé) : il
        // affiche alors son écran d'excuse, et il n'y a rien à envelopper.
        if (bridge == null || bridge.getWebView() == null) return;

        WebView webView = bridge.getWebView();
        ViewGroup parent = (ViewGroup) webView.getParent();
        if (parent == null) return;
        int position = parent.indexOfChild(webView);
        ViewGroup.LayoutParams place = webView.getLayoutParams();
        parent.removeView(webView);

        SwipeRefreshLayout balayage = new SwipeRefreshLayout(this);
        balayage.addView(webView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        parent.addView(balayage, position, place);

        balayage.setColorSchemeColors(BLEU_CHARTE);
        balayage.setDistanceToTriggerSync((int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, COURSE_DP, getResources().getDisplayMetrics()));
        // Ne s'arme que si la page est déjà tout en haut, sinon on laisse
        // le défilement normal de la WebView faire son travail. (getScrollY
        // ne voit que le document racine : dans une zone à défilement interne
        // déjà descendue, le geste reste possible — d'où la course allongée.)
        balayage.setOnChildScrollUpCallback((disposition, enfant) -> webView.getScrollY() > 0);
        balayage.setOnRefreshListener(webView::reload);

        // addWebViewListener plutôt que setWebViewClient : la roue s'arrête
        // aussi sur page d'erreur, et rien ne casse si un plugin remplace un
        // jour le client WebView de Capacitor.
        bridge.addWebViewListener(new WebViewListener() {
            @Override
            public void onPageLoaded(WebView vue) { balayage.setRefreshing(false); }

            @Override
            public void onReceivedError(WebView vue) { balayage.setRefreshing(false); }

            @Override
            public void onReceivedHttpError(WebView vue) { balayage.setRefreshing(false); }
        });
    }
}
