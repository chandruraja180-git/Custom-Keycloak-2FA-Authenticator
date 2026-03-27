<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true; section>
    <#if section = "header">
        <#-- Title from properties file -->
        <div class="grootan-header">${msg("2faTitle")}</div>
    <#elseif section = "form">
        <form id="kc-otp-login" class="form-vertical" action="${url.loginAction}" method="post">
            
            <#-- CRITICAL: This hidden input keeps the page in German/French on error -->
            <input type="hidden" name="kc_locale" value="${locale.current}"/>

            <div class="form-group">
                <#-- Instruction text from properties -->
                <p class="instruction-text">${msg("2faInstruction")}</p>
                
                <#-- The OTP Input -->
                <input id="otp" name="otp" type="text" class="grootan-input" 
                       autofocus autocomplete="off" 
                       placeholder="000000" />
            </div>

            <div class="form-actions">
                <#-- Button text from properties -->
                <input class="btn-grootan" type="submit" value="${msg('loginAction')}"/>
            </div>

            <#-- Back to Login Link -->
            <div class="back-link">
                <a href="${url.loginRestartFlowUrl}">${msg("backToLogin")}</a>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
