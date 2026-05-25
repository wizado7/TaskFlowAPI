{{- define "taskflow.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "taskflow.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name (include "taskflow.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "taskflow.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
app.kubernetes.io/name: {{ include "taskflow.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- with .Values.global.labels }}
{{ toYaml . }}
{{- end }}
{{- end -}}

{{- define "taskflow.selectorLabels" -}}
app.kubernetes.io/name: {{ include "taskflow.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "taskflow.configName" -}}
{{ include "taskflow.fullname" . }}-config
{{- end -}}

{{- define "taskflow.secretName" -}}
{{- if .Values.secrets.existingSecret -}}
{{ .Values.secrets.existingSecret }}
{{- else -}}
{{ include "taskflow.fullname" . }}-secrets
{{- end -}}
{{- end -}}

{{- define "taskflow.grafanaSecretName" -}}
{{- if .Values.monitoring.grafana.admin.existingSecret -}}
{{ .Values.monitoring.grafana.admin.existingSecret }}
{{- else -}}
{{ include "taskflow.fullname" . }}-grafana-admin
{{- end -}}
{{- end -}}

{{- define "taskflow.image" -}}
{{- $root := index . 0 -}}
{{- $image := index . 1 -}}
{{- if $root.Values.global.imageRegistry -}}
{{ printf "%s/%s:%s" ($root.Values.global.imageRegistry | trimSuffix "/") $image.repository $image.tag }}
{{- else -}}
{{ printf "%s:%s" $image.repository $image.tag }}
{{- end -}}
{{- end -}}
