<#import "template.ftl" as layout>

<@layout.registrationLayout displayMessage=true; section>

<#if section == "title">
    ${msg("otp.label")}
</#if>

<#if section == "form">

<div style="text-align:right">
    <a href="?kc_locale=en">English</a> |
    <a href="?kc_locale=fr">Français</a> |
    <a href="?kc_locale=de">Deutsch</a>
</div>

<#if message?has_content>
    <div style="color:red;">
        ${kcSanitize(message.summary)?no_esc}
    </div>
</#if>

<form action="${url.loginAction}" method="post">

    <label>${msg("otp.label")}</label><br/>

    <input type="text" name="otp"/><br/><br/>

    <button type="submit">${msg("otp.verify")}</button>

</form>

</#if>

</@layout.registrationLayout>