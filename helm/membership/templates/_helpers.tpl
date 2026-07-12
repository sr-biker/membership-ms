{{- define "membership.name" -}}
membership
{{- end -}}

{{- define "membership.selectorLabels" -}}
app.kubernetes.io/name: {{ include "membership.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "membership.labels" -}}
{{ include "membership.selectorLabels" . }}
environment: {{ .Values.environment }}
{{- end -}}

{{- define "membership.secretName" -}}
{{- if .Values.db.existingSecret -}}
{{ .Values.db.existingSecret }}
{{- else -}}
{{ include "membership.name" . }}-db
{{- end -}}
{{- end -}}
