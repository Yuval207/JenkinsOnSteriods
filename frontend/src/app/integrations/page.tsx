"use client";

import { useState } from 'react';
import { 
  Code2, 
  CheckCircle, 
  XCircle, 
  Plus, 
  ExternalLink, 
  Settings2, 
  RefreshCw, 
  Shield, 
  Send, 
  Check, 
  Loader2,
  Lock,
  GitBranch,
  BellRing,
  FolderSync
} from 'lucide-react';

type Integration = {
  id: string;
  name: string;
  category: 'SCM' | 'NOTIFICATIONS' | 'TRACKER' | 'REGISTRY';
  icon: string;
  description: string;
  status: 'CONNECTED' | 'DISCONNECTED' | 'TESTING';
  lastSync?: string;
  config: Record<string, string>;
};

const initialIntegrations: Integration[] = [
  {
    id: 'github',
    name: 'GitHub',
    category: 'SCM',
    icon: '🐙',
    description: 'Sync repositories, post automated PR annotations, and enforce merge checks.',
    status: 'CONNECTED',
    lastSync: '5 mins ago',
    config: { organization: 'yuvalsharma', prChecks: 'enabled' }
  },
  {
    id: 'slack',
    name: 'Slack DevSecOps Alerts',
    category: 'NOTIFICATIONS',
    icon: '💬',
    description: 'Send real-time alerts for critical vulnerabilities and pipeline blocks to designated channels.',
    status: 'CONNECTED',
    lastSync: '12 mins ago',
    config: { webhookUrl: 'https://hooks.slack.com/services/T00/B00/XXXX', channel: '#sec-alerts' }
  },
  {
    id: 'jira',
    name: 'Jira Software',
    category: 'TRACKER',
    icon: '🎯',
    description: 'Automatically create Jira remediation tickets with CVE details and CVSS prioritization.',
    status: 'DISCONNECTED',
    config: { host: 'https://jira.internal.corp', projectKey: 'SEC' }
  },
  {
    id: 'dockerhub',
    name: 'Docker Hub & ECR',
    category: 'REGISTRY',
    icon: '🐳',
    description: 'Pull base images and scan container layer filesystems on push triggers.',
    status: 'CONNECTED',
    lastSync: '1 hour ago',
    config: { registry: 'docker.io/systemguard' }
  },
  {
    id: 'gitlab',
    name: 'GitLab CI/CD',
    category: 'SCM',
    icon: '🦊',
    description: 'Integrate native GitLab MR security reports and pipeline status badges.',
    status: 'DISCONNECTED',
    config: {}
  },
  {
    id: 'custom-webhook',
    name: 'Custom Webhook Dispatcher',
    category: 'NOTIFICATIONS',
    icon: '⚡',
    description: 'Publish JSON event payloads on scan lifecycle events to external SIEM / SOAR tools.',
    status: 'CONNECTED',
    lastSync: 'Just now',
    config: { endpoint: 'http://localhost:8080/events/webhook' }
  }
];

export default function IntegrationsPage() {
  const [integrations, setIntegrations] = useState<Integration[]>(initialIntegrations);
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [activeModal, setActiveModal] = useState<Integration | null>(null);
  const [testingId, setTestingId] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleTestConnection = (id: string) => {
    setTestingId(id);
    setTimeout(() => {
      setTestingId(null);
      setIntegrations(prev => prev.map(item => item.id === id ? { ...item, status: 'CONNECTED', lastSync: 'Just now' } : item));
      showToast('Connection verified successfully');
    }, 1200);
  };

  const toggleStatus = (id: string) => {
    setIntegrations(prev => prev.map(item => {
      if (item.id === id) {
        const nextStatus = item.status === 'CONNECTED' ? 'DISCONNECTED' : 'CONNECTED';
        showToast(`${item.name} ${nextStatus === 'CONNECTED' ? 'connected' : 'disconnected'}`);
        return { ...item, status: nextStatus, lastSync: nextStatus === 'CONNECTED' ? 'Just now' : undefined };
      }
      return item;
    }));
  };

  const categories = [
    { label: 'All Integrations', value: 'ALL' },
    { label: 'Source Code (SCM)', value: 'SCM' },
    { label: 'Alerts & ChatOps', value: 'NOTIFICATIONS' },
    { label: 'Issue Trackers', value: 'TRACKER' },
    { label: 'Container Registries', value: 'REGISTRY' },
  ];

  const filtered = integrations.filter(i => categoryFilter === 'ALL' || i.category === categoryFilter);

  return (
    <div className="space-y-6 pb-12">
      {/* Toast */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 px-4 py-3 rounded-xl shadow-2xl flex items-center gap-2 backdrop-blur-md animate-in fade-in slide-in-from-bottom-5">
          <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0" />
          <span className="text-sm font-medium">{toastMessage}</span>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h1 className="text-display text-white">Ecosystem & Integrations</h1>
          <p className="text-zinc-400 mt-1 text-sm">
            Connect Git providers, notification webhooks, issue trackers, and container engines.
          </p>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex gap-2 overflow-x-auto pb-1">
        {categories.map(cat => (
          <button
            key={cat.value}
            onClick={() => setCategoryFilter(cat.value)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors shrink-0 ${
              categoryFilter === cat.value
                ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20'
                : 'bg-[#18181b] text-zinc-400 hover:text-zinc-200 border border-white/5'
            }`}
          >
            {cat.label}
          </button>
        ))}
      </div>

      {/* Integration Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filtered.map(item => {
          const isConnected = item.status === 'CONNECTED';
          const isTesting = testingId === item.id;

          return (
            <div 
              key={item.id}
              className={`sg-card p-5 flex flex-col justify-between transition-all ${
                isConnected ? 'border-white/10' : 'border-white/5 bg-[#121214]'
              }`}
            >
              <div>
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-xl shadow-inner">
                      {item.icon}
                    </div>
                    <div>
                      <h3 className="text-sm font-semibold text-white">{item.name}</h3>
                      <span className="text-[10px] font-mono text-zinc-500 uppercase">{item.category}</span>
                    </div>
                  </div>

                  <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-medium ${
                    isConnected 
                      ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' 
                      : 'bg-zinc-800 text-zinc-400 border border-white/5'
                  }`}>
                    <span className={`w-1.5 h-1.5 rounded-full ${isConnected ? 'bg-emerald-400' : 'bg-zinc-500'}`} />
                    {isConnected ? 'Connected' : 'Not Connected'}
                  </span>
                </div>

                <p className="text-xs text-zinc-400 leading-relaxed mb-4">
                  {item.description}
                </p>
              </div>

              <div className="pt-3 border-t border-white/5 flex items-center justify-between gap-2">
                <span className="text-[11px] text-zinc-500">
                  {item.lastSync ? `Synced ${item.lastSync}` : 'Never synced'}
                </span>

                <div className="flex items-center gap-1.5">
                  <button
                    disabled={isTesting}
                    onClick={() => handleTestConnection(item.id)}
                    className="p-1.5 text-zinc-400 hover:text-white bg-white/5 hover:bg-white/10 rounded-lg transition-colors"
                    title="Test connectivity"
                  >
                    {isTesting ? <Loader2 className="w-3.5 h-3.5 animate-spin text-indigo-400" /> : <RefreshCw className="w-3.5 h-3.5" />}
                  </button>

                  <button
                    onClick={() => toggleStatus(item.id)}
                    className={`px-3 py-1 rounded-lg text-xs font-medium transition-colors ${
                      isConnected
                        ? 'bg-zinc-800 hover:bg-rose-500/20 text-zinc-300 hover:text-rose-300'
                        : 'btn btn-primary text-xs py-1'
                    }`}
                  >
                    {isConnected ? 'Disconnect' : 'Connect'}
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
