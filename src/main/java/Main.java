void main() {
    javax.swing.SwingUtilities.invokeLater(() -> {
        new javax.swing.SwingWorker<SesionSupabase, Void>() {
            @Override
            protected SesionSupabase doInBackground() {
                //try to silently restore a previously saved session first,
                //so the doctor doesn't have to log in on every launch
                return SupabaseAuthClient.restaurarSesion();
            }

            @Override
            protected void done() {
                SesionSupabase sesion = null;
                try {
                    sesion = get();
                } catch (Exception ignored) {
                }

                //no saved session (or it's no longer valid) - show the login dialog
                if (sesion == null) {
                    sesion = LogInForm.mostrar(null);
                }

                if (sesion != null) {
                    new ReportBuilder(sesion);
                } else {
                    //user closed the login dialog without authenticating
                    System.exit(0);
                }
            }
        }.execute();
    });
}