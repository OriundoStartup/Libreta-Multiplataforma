import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Entrega el callback OAuth (org.oriundo://login-callback...) a Kotlin/Supabase
                    MainViewControllerKt.handleDeepLink(url: url.absoluteString)
                }
        }
    }
}