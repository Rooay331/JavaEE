<%@page contentType="text/html" pageEncoding="UTF-8" %>
<% String errorMessage = (String) request.getAttribute("errorMessage");
    String fullName = (String) request.getAttribute("fullName");
    String email = (String) request.getAttribute("email");
    String phone = (String) request.getAttribute("phone");
    String dob = (String) request.getAttribute("dateOfBirth");
    String gender = (String) request.getAttribute("gender");
    String fullNameError = (String) request.getAttribute("fullNameError");
    String dobError = (String) request.getAttribute("dobError");
    String genderError = (String) request.getAttribute("genderError");
    String emailError = (String) request.getAttribute("emailError");
    String phoneError = (String) request.getAttribute("phoneError");
    String passwordError = (String) request.getAttribute("passwordError");
    String confirmPasswordError = (String) request.getAttribute("confirmPasswordError");
    String agreeError = (String) request.getAttribute("agreeError");
    Object agreedObj = request.getAttribute("agreed");
    boolean agreed = false;
    if (agreedObj instanceof Boolean) {
        agreed = (Boolean) agreedObj;
    } else if (agreedObj != null) {
        String agreedText = agreedObj.toString();
        agreed = "1"
                .equals(agreedText) || "true".equalsIgnoreCase(agreedText) || "on".equalsIgnoreCase(agreedText);
    }
    if (fullName == null) {
        fullName = "";
    }
    if (email == null) {
        email = "";
    }
    if (phone == null) {
        phone = "";
    }
    if (dob == null) {
        dob = "";
    }
    if (gender == null) {
        gender = "";
    }
    String safeFullName = fullName.replace("\"", "&quot;");
    String safeEmail = email.replace("\"", "&quot;");
    String safePhone = phone.replace("\"", "&quot;");
    String safeDob = dob.replace("\"", "&quot;");
    String safeGender = gender.replace("\"", "&quot;");%>
<!doctype html>
<html lang="en">

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>CCHC Community Clinic System - Register</title>
        <link rel="stylesheet" href="<%= request.getContextPath()%>/assets/css/style.css">
    </head>

    <body>
        <header class="site-header">
            <div class="container navbar">
                <a class="brand" href="index.html">CCHC Community Clinic System</a>
                <ul class="nav-links">
                    <li><a href="index.html">Home</a></li>
                    <li><a href="login">Login</a></li>
                    <li><a class="active" href="register">Register</a></li>
                    <li><a href="clinics">Clinic List</a></li>
                </ul>
            </div>
        </header>

        <main class="container">
            <section class="form-wrap">
                <div class="form-card">
                    <div class="form-intro">
                        <h1>Patient registration</h1>
                        <p>Create an account to book appointments and join walk-in queues online.</p>
                    </div>

                    <% if (errorMessage != null && !errorMessage.trim().isEmpty()) {%>
                    <p class="section-subtitle form-global-error">
                        <%= errorMessage%>
                    </p>
                    <% }%>

                    <form id="registerForm" action="register" method="post"
                          class="form-grid" novalidate>
                        <div class="field">
                            <label for="fullName">Full name</label>
                            <input id="fullName" name="fullName" type="text" value="<%= safeFullName%>"
                                   placeholder="Chan Tai Man" aria-describedby="fullNameErrorText"
                                   class="<%=fullNameError != null ? " input-invalid" : ""%>"
                                   required>
                            <p id="fullNameErrorText" class="field-error<%=fullNameError != null ? " visible" : ""%>"><%= fullNameError == null ? "" : fullNameError%>
                            </p>
                        </div>

                        <div class="field">
                            <label for="dob">Date of birth</label>
                            <input id="dob" name="dob" type="text" value="<%= safeDob%>"
                                placeholder="12-6-2026" pattern="\d{1,2}-\d{1,2}-\d{4}|\d{4}-\d{1,2}-\d{1,2}"
                                   aria-describedby="dobErrorText" class="<%=dobError != null ? " input-invalid" : ""%>"
                                   required>
                            <p id="dobErrorText" class="field-error<%=dobError != null ? " visible" : ""%>"><%= dobError == null ? "" : dobError%>
                            </p>
                        </div>

                        <div class="field">
                            <label for="gender">Gender</label>
                            <select id="gender" name="gender" aria-describedby="genderErrorText" class="<%=genderError != null ? " input-invalid" : ""%>">
                                <option value="">Select Gender</option>
                                <option value="MALE" <%= "MALE".equals(safeGender) ? "selected" : ""%>>Male</option>
                                <option value="FEMALE" <%= "FEMALE".equals(safeGender) ? "selected" : ""%>>Female</option>
                            </select>
                            <p id="genderErrorText" class="field-error<%=genderError != null ? " visible" : ""%>"><%= genderError == null ? "" : genderError%></p>
                        </div>

                        <div class="field">
                            <label for="email">Email</label>
                            <input id="email" name="email" type="email" value="<%= safeEmail%>"
                                   placeholder="patient@example.com" aria-describedby="emailErrorText"
                                   class="<%=emailError != null ? " input-invalid" : ""%>"
                                   required>
                            <p id="emailErrorText" class="field-error<%=emailError != null ? " visible" : ""%>"><%= emailError == null ? "" : emailError%>
                            </p>
                        </div>

                        <div class="field">
                            <label for="phone">Mobile number</label>
                            <input id="phone" name="phone" type="tel" value="<%= safePhone%>"
                                   placeholder="9123 4567" aria-describedby="phoneErrorText"
                                   class="<%=phoneError != null ? " input-invalid" : ""%>"
                                   required>
                            <p id="phoneErrorText" class="field-error<%=phoneError != null ? " visible" : ""%>"><%= phoneError == null ? "" : phoneError%>
                            </p>
                        </div>

                        <div class="field">
                            <label for="password">Password</label>
                            <input id="password" name="password" type="password" minlength="8"
                                   placeholder="At least 8 characters" aria-describedby="passwordErrorText"
                                   class="<%=passwordError != null ? " input-invalid" : ""%>"
                                   required>
                            <p id="passwordErrorText" class="field-error<%=passwordError != null ? " visible" : ""%>"><%= passwordError == null ? "" : passwordError%>
                            </p>
                        </div>

                        <div class="field">
                            <label for="confirmPassword">Confirm password</label>
                            <input id="confirmPassword" name="confirmPassword" type="password" minlength="8"
                                   placeholder="Re-enter password" aria-describedby="confirmPasswordErrorText"
                                   class="<%=confirmPasswordError != null ? " input-invalid" : ""%>"
                                   required>
                            <p id="confirmPasswordErrorText"
                               class="field-error<%=confirmPasswordError != null ? " visible" : ""%>"><%=confirmPasswordError == null ? "" : confirmPasswordError%>
                            </p>
                        </div>

                        <div class="field field-full">
                            <div id="agreeRow" class="inline-row<%=agreeError != null ? " input-invalid-row" : ""%>">
                                <input id="agree" name="agree" type="checkbox" value="1"
                                       aria-describedby="agreeErrorText" class="<%=agreeError != null ? " input-invalid" : ""%>"
                                       <%= agreed ? "checked" : ""%>>
                                <label for="agree">I agree to the account and data handling
                                    terms.</label>
                            </div>
                            <p id="agreeErrorText" class="field-error<%=agreeError != null ? " visible" : ""%>"><%= agreeError == null ? "" : agreeError%>
                            </p>
                        </div>

                        <div class="field field-full form-actions">
                            <button class="btn btn-primary" type="submit">Register Account</button>
                            <a class="btn btn-secondary"
                               href="<%= request.getContextPath()%>/login">Back to Login</a>
                        </div>
                    </form>
                </div>
            </section>
        </main>

        <footer class="site-footer">
            <div class="container">After registration, you can sign in and proceed to appointment booking.</div>
        </footer>

        <script>
            (function () {
                var form = document.getElementById('registerForm');
                if (!form) {
                    return;
                }

                var fullNameInput = document.getElementById('fullName');
                var dobInput = document.getElementById('dob');
                var genderInput = document.getElementById('gender');
                var emailInput = document.getElementById('email');
                var phoneInput = document.getElementById('phone');
                var passwordInput = document.getElementById('password');
                var confirmPasswordInput = document.getElementById('confirmPassword');
                var agreeCheckbox = document.getElementById('agree');
                var agreeRow = document.getElementById('agreeRow');

                var fullNameErrorText = document.getElementById('fullNameErrorText');
                var dobErrorText = document.getElementById('dobErrorText');
                var genderErrorText = document.getElementById('genderErrorText');
                var emailErrorText = document.getElementById('emailErrorText');
                var phoneErrorText = document.getElementById('phoneErrorText');
                var passwordErrorText = document.getElementById('passwordErrorText');
                var confirmPasswordErrorText = document.getElementById('confirmPasswordErrorText');
                var agreeErrorText = document.getElementById('agreeErrorText');

                function isBlank(value) {
                    return !value || value.trim().length === 0;
                }

                function showInputError(input, errorElement, message) {
                    if (input) {
                        input.classList.add('input-invalid');
                    }
                    if (errorElement) {
                        errorElement.textContent = message;
                        errorElement.classList.add('visible');
                    }
                }

                function clearInputError(input, errorElement) {
                    if (input) {
                        input.classList.remove('input-invalid');
                    }
                    if (errorElement) {
                        errorElement.textContent = '';
                        errorElement.classList.remove('visible');
                    }
                }

                function showAgreeError(message) {
                    if (agreeCheckbox) {
                        agreeCheckbox.classList.add('input-invalid');
                    }
                    if (agreeRow) {
                        agreeRow.classList.add('input-invalid-row');
                    }
                    if (agreeErrorText) {
                        agreeErrorText.textContent = message;
                        agreeErrorText.classList.add('visible');
                    }
                }

                function clearAgreeError() {
                    if (agreeCheckbox) {
                        agreeCheckbox.classList.remove('input-invalid');
                    }
                    if (agreeRow) {
                        agreeRow.classList.remove('input-invalid-row');
                    }
                    if (agreeErrorText) {
                        agreeErrorText.textContent = '';
                        agreeErrorText.classList.remove('visible');
                    }
                }

                function validateFullName() {
                    if (!fullNameInput) {
                        return true;
                    }
                    if (isBlank(fullNameInput.value)) {
                        showInputError(fullNameInput, fullNameErrorText, 'Please enter your full name.');
                        return false;
                    }
                    clearInputError(fullNameInput, fullNameErrorText);
                    return true;
                }

                function validateDob() {
                    if (!dobInput) {
                        return true;
                    }
                    if (isBlank(dobInput.value)) {
                        showInputError(dobInput, dobErrorText, 'Please enter your date of birth.');
                        return false;
                    }
                    clearInputError(dobInput, dobErrorText);
                    return true;
                }

                function validateGender() {
                    // Gender is optional
                    return true;
                }

                function validateEmail() {
                    if (!emailInput) {
                        return true;
                    }
                    var emailValue = emailInput.value;
                    if (isBlank(emailValue)) {
                        showInputError(emailInput, emailErrorText, 'Please enter your email address.');
                        return false;
                    }
                    if (emailValue.indexOf('@') < 0) {
                        showInputError(emailInput, emailErrorText, 'Please enter a valid email address.');
                        return false;
                    }
                    clearInputError(emailInput, emailErrorText);
                    return true;
                }

                function validatePhone() {
                    if (!phoneInput) {
                        return true;
                    }
                    var phoneValue = phoneInput.value;
                    var phoneDigits = phoneValue.replace(/\D/g, '');
                    if (isBlank(phoneValue)) {
                        showInputError(phoneInput, phoneErrorText, 'Please enter your mobile number.');
                        return false;
                    }
                    if (phoneDigits.length !== 8) {
                        showInputError(phoneInput, phoneErrorText, 'The mobile number must be exactly 8 digits.');
                        return false;
                    }
                    clearInputError(phoneInput, phoneErrorText);
                    return true;
                }

                function validatePassword() {
                    if (!passwordInput) {
                        return true;
                    }
                    var passwordValue = passwordInput.value;
                    if (isBlank(passwordValue)) {
                        showInputError(passwordInput, passwordErrorText, 'Please enter your password.');
                        return false;
                    }
                    if (passwordValue.length < 8) {
                        showInputError(passwordInput, passwordErrorText, 'Password must be at least 8 characters.');
                        return false;
                    }
                    clearInputError(passwordInput, passwordErrorText);
                    return true;
                }

                function validateConfirmPassword() {
                    if (!confirmPasswordInput) {
                        return true;
                    }
                    var confirmValue = confirmPasswordInput.value;
                    if (isBlank(confirmValue)) {
                        showInputError(confirmPasswordInput, confirmPasswordErrorText, 'Please confirm your password.');
                        return false;
                    }
                    if (passwordInput && confirmValue !== passwordInput.value) {
                        showInputError(confirmPasswordInput, confirmPasswordErrorText, 'Confirm password does not match.');
                        return false;
                    }
                    clearInputError(confirmPasswordInput, confirmPasswordErrorText);
                    return true;
                }

                function validateAgree() {
                    if (!agreeCheckbox) {
                        return true;
                    }
                    if (!agreeCheckbox.checked) {
                        showAgreeError('You must agree to the account and data handling terms.');
                        return false;
                    }
                    clearAgreeError();
                    return true;
                }

                function validateForm() {
                    var isValid = true;
                    if (!validateFullName()) {
                        isValid = false;
                    }
                    if (!validateDob()) {
                        isValid = false;
                    }
                    if (!validateGender()) {
                        isValid = false;
                    }
                    if (!validateEmail()) {
                        isValid = false;
                    }
                    if (!validatePhone()) {
                        isValid = false;
                    }
                    if (!validatePassword()) {
                        isValid = false;
                    }
                    if (!validateConfirmPassword()) {
                        isValid = false;
                    }
                    if (!validateAgree()) {
                        isValid = false;
                    }
                    return isValid;
                }

                form.addEventListener('submit', function (event) {
                    if (!validateForm()) {
                        event.preventDefault();
                    }
                });

                if (fullNameInput) {
                    fullNameInput.addEventListener('input', validateFullName);
                }
                if (dobInput) {
                    dobInput.addEventListener('change', validateDob);
                }
                if (genderInput) {
                    genderInput.addEventListener('change', validateGender);
                }
                if (emailInput) {
                    emailInput.addEventListener('input', validateEmail);
                }
                if (phoneInput) {
                    phoneInput.addEventListener('input', validatePhone);
                }
                if (passwordInput) {
                    passwordInput.addEventListener('input', function () {
                        validatePassword();
                        if (confirmPasswordInput && !isBlank(confirmPasswordInput.value)) {
                            validateConfirmPassword();
                        }
                    });
                }
                if (confirmPasswordInput) {
                    confirmPasswordInput.addEventListener('input', validateConfirmPassword);
                }
                if (agreeCheckbox) {
                    agreeCheckbox.addEventListener('change', validateAgree);
                }
            })();
        </script>
    </body>

</html>

