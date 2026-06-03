{{- define "task4.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "task4.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name (include "task4.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "task4.labels" -}}
app.kubernetes.io/name: {{ include "task4.name" . }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "task4.selectorLabels" -}}
app.kubernetes.io/name: {{ include "task4.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "task4.backendName" -}}
{{- printf "%s-backend" (include "task4.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "task4.frontendName" -}}
{{- printf "%s-frontend" (include "task4.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "task4.gatewayName" -}}
{{- printf "%s-gateway" (include "task4.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "task4.routeName" -}}
{{- printf "%s-route" (include "task4.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "task4.redirectRouteName" -}}
{{- printf "%s-redirect" (include "task4.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "task4.configMapName" -}}
{{- printf "%s-config" (include "task4.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "task4.cnpgClusterName" -}}
{{- .Values.cnpg.cluster.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "task4.cnpgAppSecretName" -}}
{{- printf "%s-app-auth" (include "task4.cnpgClusterName" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "task4.gatewayTlsSecretName" -}}
{{- if .Values.gateway.tls.secretName -}}
{{- .Values.gateway.tls.secretName -}}
{{- else -}}
{{- .Values.certManager.certificate.secretName -}}
{{- end -}}
{{- end -}}
