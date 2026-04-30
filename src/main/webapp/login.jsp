<%@page contentType="text/html" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="en">

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>CCHC Community Clinic System - Login</title>
        <link rel="stylesheet" href="assets/css/style.css">
    </head>

    <body>
        <header class="site-header">
            <div class="container navbar">
                <a class="brand" href="index.html">CCHC Community Clinic System</a>
                <ul class="nav-links">
                    <li><a href="index.html">Home</a></li>
                    <li><a class="active" href="login">Login</a></li>
                    <li><a href="register">Register</a></li>
                    <li><a href="clinics">Clinic List</a></li>
                </ul>
            </div>
        </header>

        <main class="container">
            <section class="form-wrap">
                <div class="form-card">
                    <div class="form-intro">
                        <h1>Sign in to your account</h1>
                    </div>

                    <form id="loginForm" action="login" method="post" class="form-grid" novalidate>
                        <div class="field field-full">
                            <label for="identity">Email, mobile number, or full name</label>
                            <input id="identity" name="username" type="text" placeholder="Email, phone, or full name" autocomplete="username">
                        </div>

                        <div class="field field-full">
                            <label for="password">Password</label>
                            <input id="password" name="password" type="password" placeholder="Password">
                        </div>

                        <div class="field field-full form-actions">
                            <button class="btn btn-primary" type="submit">Login</button>
                        </div>
                    </form>

                    <p>
                        New to CCHC? Register via the patient portal.
                        <a href="register">Go to registration page</a>.
                    </p>
                </div>
            </section>
        </main>

        <footer class="site-footer">
            <div class="container">Need help? Contact your nearest clinic from the public clinic list page.</div>
        </footer>
    </body>

</html>