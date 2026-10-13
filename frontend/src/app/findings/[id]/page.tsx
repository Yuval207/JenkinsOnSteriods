"use client";

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  ChevronLeft, 
  ShieldCheck, 
  FileCode2, 
  SearchCode, 
  BookOpen, 
  Bug, 
  Loader2, 
  Sparkles, 
  CheckCircle2, 
  Wrench, 
  GitCommit 
} from 'lucide-react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { formatDistanceToNow } from 'date-fns';
import { useState } from 'react';
import axios from 'axios';

type Finding = {
  id: string;
  projectId: string;
  scanRunId: string;
  title: string;
  description: string;
  severity: string;
  status: string;
  source: string;
  findingType: string;
  filePath: string;
  lineStart: number | null;
  lineEnd: number | null;
  codeSnippet: string | null;
  ruleId: string;
  cveId: string;
  cweId: string;
  packageName: string;
  installedVersion: string;
  fixedVersion: string;
  remediationAdvice: string;
  remediationStatus: string;
  contextualRiskScore: number;
  firstSeen: string;
  lastSeen: string;
  evidence: Record<string, any>;
};

export default function FindingDetailsPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();
  const [selectedStatus, setSelectedStatus] = useState('');
  const [fixResult, setFixResult] = useState<any>(null);
  const [fixMessage, setFixMessage] = useState<string | null>(null);

  const { data: finding, isLoading } = useQuery({
    queryKey: ['finding', id],
    queryFn: async () => {
      const res = await axios.get(`/api/v1/findings/${id}`);
      return res.data as Finding;
    }
  });

  const updateStatusMutation = useMutation({
    mutationFn: async (newStatus: string) => {
      const res = await axios.patch(`/api/v1/findings/${id}`, {
        status: newStatus,
        reason: 'Manual update from UI'
      });
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['finding', id] });
      queryClient.invalidateQueries({ queryKey: ['all_findings'] });
    }
  });

  const autoFixMutation = useMutation({
    mutationFn: async () => {
      const res = await axios.post(`/api/v1/findings/${id}/auto-fix`);
      return res.data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['finding', id] });
      queryClient.invalidateQueries({ queryKey: ['all_findings'] });
      if (data.applied) {
        setFixResult(data);
        setFixMessage('Security fix was successfully written to the code on disk!');
        setTimeout(() => setFixMessage(null), 8000);
      } else {
        alert(data.errorMessage || 'AI Auto-Fix could not generate a patch.');
      }
    },
    onError: (err: any) => {
      alert('AI Auto-Fix request failed: ' + (err.response?.data?.message || err.message));
    }
  });

  if (isLoading || !finding) {
    return <div className="flex justify-center items-center h-64"><Loader2 className="w-8 h-8 animate-spin text-zinc-500" /></div>;
  }

  const severityBadgeClass = {
    CRITICAL: 'badge-critical',
    HIGH: 'badge-high',
    MEDIUM: 'badge-medium',
    LOW: 'badge-low',
    INFO: 'badge-neutral',
  }[finding.severity] || 'badge-neutral';

  const isResolved = finding.status === 'RESOLVED';
  const autoFixDiff = fixResult?.diff || finding.evidence?.autoFixDiff;
  const autoFixExplanation = fixResult?.explanation || finding.evidence?.autoFixExplanation;

  return (
    <div className="space-y-6 pb-12">
      {/* Success Notification */}
      {fixMessage && (
        <div className="p-4 bg-emerald-500/10 border border-emerald-500/30 rounded-xl flex items-center justify-between text-emerald-400 text-sm animate-in fade-in slide-in-from-top duration-300">
          <div className="flex items-center gap-2.5">
            <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
            <span className="font-medium">{fixMessage}</span>
          </div>
          <button onClick={() => setFixMessage(null)} className="text-zinc-500 hover:text-zinc-300 text-xs">Dismiss</button>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col gap-4">
        <Link href="/findings" className="inline-flex items-center gap-2 text-xs font-medium text-zinc-500 hover:text-indigo-400 transition-colors">
          <ChevronLeft className="w-4 h-4" />
          Back to Inbox
        </Link>
        <div className="flex items-start justify-between flex-wrap gap-4">
          <div>
            <div className="flex items-center gap-3 mb-2 flex-wrap">
              <span className={`badge ${severityBadgeClass} text-sm px-3 py-1 uppercase tracking-wider`}>{finding.severity}</span>
              {finding.cweId && <span className="text-zinc-500 text-sm font-mono">{finding.cweId}</span>}
              {finding.cveId && <span className="text-zinc-500 text-sm font-mono">{finding.cveId}</span>}
              <span className="badge badge-neutral bg-white/5 border-white/10 text-zinc-300 font-mono text-xs">{finding.source}</span>
              {finding.remediationStatus === 'PATCH_APPLIED' && (
                <span className="badge bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-xs flex items-center gap-1 font-mono">
                  <Sparkles className="w-3 h-3 text-amber-300" /> AI PATCHED
                </span>
              )}
            </div>
            <h1 className="text-display text-white">{finding.title}</h1>
            <div className="flex items-center gap-4 mt-3 text-sm text-zinc-400">
              <span className="flex items-center gap-1.5"><Bug className="w-4 h-4" /> {finding.findingType}</span>
              {finding.filePath && (
                <span className="flex items-center gap-1.5">
                  <FileCode2 className="w-4 h-4" /> 
                  {finding.filePath}{finding.lineStart ? `:${finding.lineStart}` : ''}
                </span>
              )}
            </div>
          </div>
          
          <div className="flex items-center gap-3 shrink-0">
            {/* AI Auto-Fix Button */}
            <button
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-all flex items-center gap-2 shadow-lg ${
                isResolved 
                  ? 'bg-zinc-800 text-zinc-400 border border-white/10 cursor-default'
                  : 'bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white shadow-indigo-500/20'
              }`}
              onClick={() => autoFixMutation.mutate()}
              disabled={autoFixMutation.isPending || isResolved}
            >
              {autoFixMutation.isPending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin text-white" />
                  Rewriting Code...
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4 text-amber-300" />
                  {isResolved ? 'Patched in Code' : 'AI Auto-Fix & Apply to Code'}
                </>
              )}
            </button>

            <button 
              className="btn-secondary text-emerald-400 hover:text-emerald-300 hover:border-emerald-500/30"
              onClick={() => updateStatusMutation.mutate('RESOLVED')}
              disabled={updateStatusMutation.isPending || isResolved}
            >
              <ShieldCheck className="w-4 h-4" /> 
              {isResolved ? 'Resolved' : 'Mark Resolved'}
            </button>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
        <div className="lg:col-span-2 space-y-6">
          {/* Details & Code */}
          <div className="sg-card">
            <div className="p-4 border-b border-white/5 bg-[#18181b]/50 flex items-center gap-2 text-sm font-medium text-white">
              <SearchCode className="w-4 h-4 text-indigo-400" /> Trace Context
            </div>
            <div className="p-5">
              <p className="text-zinc-300 text-sm leading-relaxed mb-6">
                {finding.description || 'No description available.'}
              </p>
              
              {finding.codeSnippet && (
                <div className="code-block">
                  <div className="code-block-header justify-between">
                    <div className="flex items-center gap-2">
                      <FileCode2 className="w-4 h-4 text-zinc-500" />
                      <span>{finding.filePath}</span>
                    </div>
                    {finding.lineStart && (
                      <span className="text-zinc-600">Lines {finding.lineStart}-{finding.lineEnd || finding.lineStart}</span>
                    )}
                  </div>
                  <div className="code-block-body">
                    <pre><code>{finding.codeSnippet}</code></pre>
                  </div>
                </div>
              )}

              {finding.packageName && (
                <div className="mt-4 p-4 bg-zinc-900/50 rounded-lg border border-white/5 space-y-2">
                  <h4 className="text-xs uppercase tracking-wider text-zinc-500 mb-3">Package Information</h4>
                  <div className="grid grid-cols-3 gap-4 text-sm">
                    <div>
                      <span className="text-zinc-500 text-xs">Package</span>
                      <div className="text-zinc-200 font-mono">{finding.packageName}</div>
                    </div>
                    <div>
                      <span className="text-zinc-500 text-xs">Installed</span>
                      <div className="text-red-400 font-mono">{finding.installedVersion || '—'}</div>
                    </div>
                    <div>
                      <span className="text-zinc-500 text-xs">Fixed In</span>
                      <div className="text-emerald-400 font-mono">{finding.fixedVersion || 'No fix available'}</div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* AI Code Patch & Diff */}
          {autoFixDiff && (
            <div className="sg-card border-indigo-500/30 shadow-lg shadow-indigo-500/5">
              <div className="p-4 border-b border-white/5 bg-gradient-to-r from-indigo-500/10 to-purple-500/10 flex items-center justify-between">
                <div className="flex items-center gap-2 text-sm font-medium text-white">
                  <Sparkles className="w-4 h-4 text-amber-300" />
                  <span>AI Applied Code Remediation</span>
                </div>
                <span className="badge bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-xs">
                  WRITTEN TO DISK
                </span>
              </div>
              <div className="p-5 space-y-4">
                {autoFixExplanation && (
                  <p className="text-zinc-300 text-sm leading-relaxed bg-white/5 p-3 rounded-lg border border-white/5">
                    {autoFixExplanation}
                  </p>
                )}
                <div className="code-block">
                  <div className="code-block-header justify-between">
                    <div className="flex items-center gap-2 text-xs font-mono text-zinc-400">
                      <GitCommit className="w-4 h-4 text-indigo-400" />
                      <span>Unified Diff</span>
                    </div>
                  </div>
                  <div className="code-block-body font-mono text-xs overflow-x-auto">
                    <pre className="p-4 space-y-0.5">
                      {autoFixDiff.split('\n').map((line: string, idx: number) => {
                        let lineStyle = 'text-zinc-400';
                        if (line.startsWith('+') && !line.startsWith('+++')) lineStyle = 'text-emerald-400 bg-emerald-500/10 px-1 rounded block';
                        else if (line.startsWith('-') && !line.startsWith('---')) lineStyle = 'text-red-400 bg-red-500/10 px-1 rounded block';
                        else if (line.startsWith('@@') || line.startsWith('---') || line.startsWith('+++')) lineStyle = 'text-indigo-400 font-semibold block';
                        return <span key={idx} className={lineStyle}>{line}</span>;
                      })}
                    </pre>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Remediation */}
          {finding.remediationAdvice && (
            <div className="sg-card">
              <div className="p-4 border-b border-white/5 bg-[#18181b]/50 flex items-center gap-2 text-sm font-medium text-white">
                <BookOpen className="w-4 h-4 text-emerald-400" /> Remediation Path
              </div>
              <div className="p-5 text-sm text-zinc-300 leading-relaxed">
                <p>{finding.remediationAdvice}</p>
              </div>
            </div>
          )}
        </div>

        {/* Sidebar metadata */}
        <div className="space-y-6">
          <div className="sg-card p-5">
            <h3 className="text-sm font-semibold text-white mb-4">Lifecycle State</h3>
            <div className="space-y-4">
              <div>
                <span className="text-[10px] text-zinc-500 uppercase tracking-wider block mb-1">Status</span>
                <span className={`badge ${finding.status === 'OPEN' ? 'badge-critical border-none bg-red-500/20 text-red-400' : 'badge-success border-none bg-emerald-500/20 text-emerald-400'}`}>
                  {finding.status}
                </span>
              </div>
              <div>
                <span className="text-[10px] text-zinc-500 uppercase tracking-wider block mb-1">First Detected</span>
                <span className="text-sm text-zinc-300">
                  {finding.firstSeen ? formatDistanceToNow(new Date(finding.firstSeen), { addSuffix: true }) : 'Unknown'}
                </span>
              </div>
              <div>
                <span className="text-[10px] text-zinc-500 uppercase tracking-wider block mb-1">Last Validated</span>
                <span className="text-sm text-zinc-300">
                  {finding.lastSeen ? formatDistanceToNow(new Date(finding.lastSeen), { addSuffix: true }) : 'Unknown'}
                </span>
              </div>
              {finding.contextualRiskScore != null && (
                <div>
                  <span className="text-[10px] text-zinc-500 uppercase tracking-wider block mb-1">Risk Score</span>
                  <span className="text-sm text-zinc-300 font-mono">{finding.contextualRiskScore.toFixed(1)}/10</span>
                </div>
              )}
            </div>

            <div className="mt-6 pt-6 border-t border-white/5">
              <label className="text-[10px] text-zinc-500 uppercase tracking-wider block mb-2">Manual Override</label>
              <select 
                className="sg-select w-full mb-3" 
                value={selectedStatus || finding.status}
                onChange={e => setSelectedStatus(e.target.value)}
              >
                <option value="OPEN">Open (Action Required)</option>
                <option value="RESOLVED">Resolved (Fixed)</option>
                <option value="FALSE_POSITIVE">False Positive</option>
                <option value="ACCEPTED_RISK">Accepted Risk</option>
              </select>
              <button 
                className="btn-primary w-full justify-center"
                onClick={() => updateStatusMutation.mutate(selectedStatus || finding.status)}
                disabled={updateStatusMutation.isPending}
              >
                {updateStatusMutation.isPending ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : 'Update Status'}
              </button>
            </div>
          </div>

          {/* Evidence */}
          {finding.evidence && Object.keys(finding.evidence).length > 0 && (
            <div className="sg-card p-5">
              <h3 className="text-sm font-semibold text-white mb-4">Evidence</h3>
              <div className="space-y-3 text-xs font-mono">
                {Object.entries(finding.evidence).map(([key, val]) => (
                  <div key={key}>
                    <span className="text-zinc-500">{key}:</span>
                    <div className="text-zinc-300 mt-0.5 break-all">
                      {typeof val === 'object' ? JSON.stringify(val, null, 2) : String(val)}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
