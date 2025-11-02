<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=messagesPerField.exists('global') displayRequiredFields=true; section>
    <#if section = "header">
        <#if messageHeader??>
            ${kcSanitize(msg("${messageHeader}"))?no_esc}
        <#else>
            ${msg("registerTitle")}
        </#if>
    <#elseif section = "form">
        <form id="kc-register-form" class="${properties.kcFormClass!}" action="${url.registrationAction}" method="post">
            <div class="${properties.kcFormCardClass!}">
                <div class="${properties.kcFormGroupClass!}">
                    <p>
                        <button type="button"
                                id="open-verification"
                                class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!}"
                                onclick="openVerification()">
                            Singnup with Minecraft
                        </button>
                    </p>
                    <#if user_code?has_content || verificationUrl?has_content>
                        <p>
                            <#if verificationUrl?has_content>
                                <a href="${verificationUrl}" target="_blank" rel="noopener">${verificationUrl}</a>
                            </#if>
                            <#if user_code?has_content>
                                <br/>
                                Code: <strong>${user_code}</strong>
                            </#if>
                        </p>
                    </#if>
                    <#if expired?? && expired>
                        <div class="${properties.kcFeedbackErrorIcon!}">
                            Device code abgelaufen. Bitte neu starten.
                        </div>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                        <span><a href="${url.loginUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a></span>
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doRegister")}"/>
                </div>
            </div>
        </form>
        <script>
            const verificationUrl = "${(verificationUrl!"")?js_string}";
            function openVerification(){
                if (verificationUrl) {
                    window.open(verificationUrl, '_blank', 'noopener');
                }
            }

            (function(){
                var intervalSec = parseInt("${(interval!5)?c}", 10);
                if (!intervalSec || intervalSec < 1) intervalSec = 5;
                var expiresAtStr = "${(expiresAt!"")?js_string}";
                var expiresAt = expiresAtStr ? parseInt(expiresAtStr, 10) : 0;
                var expiredFlag = ${((expired?? && expired)?string("true","false"))?no_esc};
                if (expiredFlag) return;

                function submitPoll(){
                    var now = Date.now();
                    if (expiresAt && now >= expiresAt){
                        // show expired message without submitting
                        var msg = document.createElement('div');
                        msg.className = '${properties.kcFeedbackErrorIcon!}';
                        msg.textContent = 'Device code abgelaufen. Bitte neu starten.';
                        document.getElementById('kc-content').prepend(msg);
                        return;
                    }
                    var form = document.getElementById('kc-register-form');
                    return;
                    if (form) form.submit();
                }

                // Initial delay equals interval, then periodic
                setTimeout(function tick(){
                    submitPoll();
                    // schedule next only if not yet expired (server will re-render this script until success)
                    var now = Date.now();
                    if (!expiresAt || now < expiresAt){
                        setTimeout(tick, intervalSec * 1000);
                    }
                }, intervalSec * 1000);
            })();
        </script>
    </#if>
</@layout.registrationLayout>
