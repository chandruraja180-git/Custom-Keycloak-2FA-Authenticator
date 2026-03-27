<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true; section>
    <#if section = "header">
        <div class="grootan-header">${msg("loginTitle")}</div>
    <#elseif section = "form">
        <div id="kc-form">
          <div id="kc-form-wrapper">
            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                
                <#-- This hidden input keeps the language sticky on error -->
                <input type="hidden" name="kc_locale" value="${locale.current}"/>

                <div class="form-group">
                    <label for="username" class="grootan-label">${msg("usernameOrEmail")}</label>
                    <input tabindex="1" id="username" name="username" value="${(login.username!'')}"  type="text" autofocus autocomplete="off" />
                </div>

                <div class="form-group">
                    <label for="password" class="grootan-label">${msg("password")}</label>
                    <input tabindex="2" id="password" name="password" type="password" autocomplete="off" />
                </div>

                <div id="kc-form-buttons" class="form-group">
                    <input tabindex="4" class="btn-grootan" name="login" id="kc-login" type="submit" value="${msg('loginAction')}"/>
                </div>
            </form>
          </div>
        </div>
    </#if>
</@layout.registrationLayout>            


