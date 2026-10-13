"use client";

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  ShieldAlert, 
  CheckCircle, 
  Search, 
  Filter, 
  ChevronRight, 
  Loader2, 
  Check, 
  XCircle, 
  ShieldCheck, 
  AlertTriangle,
  RotateCcw,
  Sparkles
} from 'lucide-react';
import { useState } from 'react';
import axios from 'axios';

type Finding = {
  id: string;
  projectId: string;
  title: string;
  severity: string;
  status: string;
  source: string;
  filePath: string;
  ruleId: string;
  cveId: string;
  cweId: string;
  remediationStatus?: string;
};

type Project = { id: string; name: string };

export default function FindingsPage() {
  const [filter, setFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [resolvingId, setResolvingId] = useState<string | null>(null);
  const [aiFixingId, setAiFixingId] = useState<string | null>(null);
  const [isBulkFixing, setIsBulkFixing] = useState(false);
  const [successToast, setSuccessToast] = useState<string | null>(null);

  const queryClient = useQueryClient();

  const { data: projectsData } = useQuery({
    queryKey: ['projects'],
    queryFn: async () => {
      const res = await axios.get('/api/v1/projects');
      return res.data.content as Project[];
    }
  });
  const projectMap = new Map((projectsData || []).map(p => [p.id, p.name]));

  // Fetch findings for all projects
  const { data: allFindings, isLoading } = useQuery({
    queryKey: ['all_findings', projectsData],
    queryFn: async () => {
      if (!projectsData || projectsData.length === 0) return [];
      const results: Finding[] = [];
      for (const p of projectsData) {
        try {
          const res = await axios.get(`/api/v1/projects/${p.id}/findings?size=100`);
          results.push(...(res.data.content as Finding[]));
        } catch { /* skip */ }
      }
      return results;
    },
    enabled: !!projectsData && projectsData.length > 0,
  });

  // Direct Resolution Mutation
  const updateStatusMutation = useMutation({
    mutationFn: async ({ findingId, status, reason }: { findingId: string; status: string; reason: string }) => {
      const res = await axios.patch(`/api/v1/findings/${findingId}`, {
        status,
        reason
      });
      return res.data;
    },
    onMutate: async ({ findingId, status }) => {
      setResolvingId(findingId);
    },
    onSuccess: (data, variables) => {
      setResolvingId(null);
      setSuccessToast(`Finding marked as ${variables.status}`);
      setTimeout(() => setSuccessToast(null), 3000);
      queryClient.invalidateQueries({ queryKey: ['all_findings'] });
    },
    onError: (err) => {
      setResolvingId(null);
      alert('Failed to update finding status: ' + err);
    }
  });

  // Single AI Auto-Fix Mutation
  const aiFixMutation = useMutation({
    mutationFn: async (findingId: string) => {
      const res = await axios.post(`/api/v1/findings/${findingId}/auto-fix`);
      return res.data;
    },
    onMutate: (findingId) => {
      setAiFixingId(findingId);
    },
    onSuccess: (data) => {
      setAiFixingId(null);
      if (data.applied) {
        setSuccessToast(`✨ AI fix applied to code & finding resolved!`);
      } else {
        alert(data.errorMessage || 'Could not apply auto-fix');
      }
      setTimeout(() => setSuccessToast(null), 4000);
      queryClient.invalidateQueries({ queryKey: ['all_findings'] });
    },
    onError: (err: any) => {
      setAiFixingId(null);
      alert('AI Fix failed: ' + (err.response?.data?.message || err.message));
    }
  });

  // Bulk AI Auto-Fix Mutation
  const bulkFixMutation = useMutation({
    mutationFn: async () => {
      if (!projectsData) return 0;
      let total = 0;
      for (const p of projectsData) {
        try {
          const res = await axios.post(`/api/v1/projects/${p.id}/auto-fix-all`);
          total += (res.data || []).length;
        } catch { /* skip */ }
      }
      return total;
    },
    onMutate: () => setIsBulkFixing(true),
    onSuccess: (total) => {
      setIsBulkFixing(false);
      setSuccessToast(`✨ AI automatically remediated ${total} vulnerabilities across repositories!`);
      setTimeout(() => setSuccessToast(null), 5000);
      queryClient.invalidateQueries({ queryKey: ['all_findings'] });
    },
    onError: (err) => {
      setIsBulkFixing(false);
      alert('Bulk fix failed: ' + err);
    }
  });

  const findings = allFindings || [];
  const filteredFindings = findings
    .filter(f => filter === 'ALL' ? true : f.severity === filter)
    .filter(f => statusFilter === 'ALL' ? true : f.status === statusFilter)
    .filter(f => {
      if (!search) return true;
      const s = search.toLowerCase();
      return f.title.toLowerCase().includes(s)
        || f.filePath?.toLowerCase().includes(s)
        || f.cveId?.toLowerCase().includes(s)
        || f.ruleId?.toLowerCase().includes(s);
    });

  const getSeverityBadge = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return <span className="badge badge-critical">Critical</span>;
      case 'HIGH': return <span className="badge badge-high">High</span>;
      case 'MEDIUM': return <span className="badge badge-medium">Medium</span>;
      case 'LOW': return <span className="badge badge-low">Low</span>;
      default: return <span className="badge badge-neutral">{severity}</span>;
    }
  };

  const openCount = findings.filter(f => f.status === 'OPEN').length;
  const resolvedCount = findings.filter(f => f.status === 'RESOLVED').length;

  return (
    <div className="space-y-6 pb-12">
      {/* Toast */}
      {successToast && (
        <div className="fixed bottom-6 right-6 z-50 bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 px-4 py-3 rounded-xl shadow-2xl flex items-center gap-2 backdrop-blur-md animate-in fade-in slide-in-from-bottom-5">
          <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0" />
          <span className="text-sm font-medium">{successToast}</span>
        </div>
      )}

      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h1 className="text-display text-white">Vulnerability Inbox</h1>
          <p className="text-zinc-400 mt-1 text-sm">Triage, investigate, and resolve findings across all scanners in real time.</p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => bulkFixMutation.mutate()}
            disabled={isBulkFixing || openCount === 0}
            className="px-3.5 py-1.5 rounded-lg bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white text-xs font-medium flex items-center gap-1.5 shadow-md shadow-indigo-500/20 transition-all disabled:opacity-50 cursor-pointer"
            title="Automatically remediate all vulnerabilities across code repositories"
          >
            {isBulkFixing ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <Sparkles className="w-3.5 h-3.5 text-amber-300" />
            )}
            AI Auto-Fix All
          </button>
          <div className="px-3 py-1.5 rounded-lg bg-red-500/10 border border-red-500/20 text-xs font-medium text-red-400 flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
            {openCount} Open Issues
          </div>
          <div className="px-3 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-xs font-medium text-emerald-400 flex items-center gap-1.5">
            <CheckCircle className="w-3.5 h-3.5" />
            {resolvedCount} Resolved
          </div>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center items-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-zinc-500" />
        </div>
      ) : (
        <div className="sg-card overflow-hidden flex flex-col mt-6">
          {/* Toolbar */}
          <div className="p-3 border-b border-white/5 flex flex-col md:flex-row gap-3 justify-between items-center bg-[#18181b]/50">
            <div className="relative w-full md:w-96">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
              <input 
                type="text" 
                placeholder="Filter findings, CVEs, or files..." 
                className="w-full bg-[#121214] border border-white/10 rounded-lg text-sm text-white pl-9 pr-3 h-9 placeholder:text-zinc-500 outline-none focus:border-indigo-500/50"
                value={search}
                onChange={e => setSearch(e.target.value)}
              />
            </div>
            
            <div className="flex flex-wrap items-center gap-2">
              <div className="flex items-center gap-1.5 bg-[#121214] border border-white/10 rounded-lg px-2 h-9">
                <Filter className="w-3.5 h-3.5 text-zinc-500" />
                <select 
                  value={filter}
                  onChange={(e) => setFilter(e.target.value)}
                  className="bg-transparent border-none text-xs text-zinc-300 focus:ring-0 outline-none cursor-pointer"
                >
                  <option value="ALL" className="bg-[#18181b]">All Severities</option>
                  <option value="CRITICAL" className="bg-[#18181b]">Critical Only</option>
                  <option value="HIGH" className="bg-[#18181b]">High Only</option>
                  <option value="MEDIUM" className="bg-[#18181b]">Medium Only</option>
                  <option value="LOW" className="bg-[#18181b]">Low Only</option>
                </select>
              </div>

              <div className="flex items-center gap-1.5 bg-[#121214] border border-white/10 rounded-lg px-2 h-9">
                <ShieldCheck className="w-3.5 h-3.5 text-zinc-500" />
                <select 
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="bg-transparent border-none text-xs text-zinc-300 focus:ring-0 outline-none cursor-pointer"
                >
                  <option value="ALL" className="bg-[#18181b]">All Statuses</option>
                  <option value="OPEN" className="bg-[#18181b]">Open Only</option>
                  <option value="RESOLVED" className="bg-[#18181b]">Resolved Only</option>
                  <option value="FALSE_POSITIVE" className="bg-[#18181b]">False Positive</option>
                  <option value="MITIGATED" className="bg-[#18181b]">Mitigated</option>
                </select>
              </div>
            </div>
          </div>

          {/* List */}
          <div className="overflow-x-auto">
            <table className="sg-table">
              <thead>
                <tr>
                  <th className="w-[100px]">Severity</th>
                  <th>Finding</th>
                  <th>Project</th>
                  <th>Scanner</th>
                  <th className="w-[120px]">Status</th>
                  <th className="w-[200px] text-right pr-4">Quick Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredFindings.map((finding) => {
                  const isCurrentResolving = resolvingId === finding.id;
                  const isOpen = finding.status === 'OPEN';

                  return (
                    <tr 
                      key={finding.id} 
                      className="group hover:bg-white/[0.02] transition-colors relative"
                    >
                      <td onClick={() => window.location.href = `/findings/${finding.id}`} className="cursor-pointer">
                        {getSeverityBadge(finding.severity)}
                      </td>
                      <td onClick={() => window.location.href = `/findings/${finding.id}`} className="cursor-pointer">
                        <div className="flex flex-col">
                          <span className="font-medium text-zinc-200 group-hover:text-indigo-400 transition-colors">
                            {finding.title}
                          </span>
                          <span className="text-xs font-mono text-zinc-500 mt-0.5 max-w-[340px] truncate">
                            {finding.filePath || finding.cveId || finding.ruleId}
                          </span>
                        </div>
                      </td>
                      <td onClick={() => window.location.href = `/findings/${finding.id}`} className="cursor-pointer">
                        <span className="px-2 py-1 bg-white/5 border border-white/5 rounded text-xs text-zinc-300">
                          {projectMap.get(finding.projectId) || finding.projectId.substring(0, 8)}
                        </span>
                      </td>
                      <td onClick={() => window.location.href = `/findings/${finding.id}`} className="cursor-pointer">
                        <span className="text-xs font-mono text-zinc-400">
                          {finding.source}
                        </span>
                      </td>
                      <td onClick={() => window.location.href = `/findings/${finding.id}`} className="cursor-pointer">
                        <div className="flex items-center gap-1.5 flex-wrap">
                          {isOpen ? (
                            <div className="w-2 h-2 rounded-full bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.6)]" />
                          ) : (
                            <CheckCircle className="w-3.5 h-3.5 text-emerald-500" />
                          )}
                          <span className={`text-xs font-medium ${isOpen ? 'text-zinc-300' : 'text-emerald-400'}`}>
                            {finding.status}
                          </span>
                          {finding.remediationStatus === 'PATCH_APPLIED' && (
                            <span className="text-[10px] px-1.5 py-0.5 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded font-mono flex items-center gap-0.5">
                              <Sparkles className="w-2.5 h-2.5 text-amber-300" /> AI Patched
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="text-right pr-4">
                        <div className="flex items-center justify-end gap-1.5" onClick={e => e.stopPropagation()}>
                          {isOpen ? (
                            <>
                              <button
                                disabled={aiFixingId === finding.id || isCurrentResolving}
                                onClick={() => aiFixMutation.mutate(finding.id)}
                                className="px-2.5 py-1 text-xs font-medium bg-gradient-to-r from-indigo-500/15 to-purple-500/15 hover:from-indigo-500/25 hover:to-purple-500/25 text-indigo-300 border border-indigo-500/30 rounded-md transition-all flex items-center gap-1 shadow-sm cursor-pointer"
                                title="AI Auto-Fix: write security patch directly to repository code"
                              >
                                {aiFixingId === finding.id ? <Loader2 className="w-3 h-3 animate-spin text-indigo-400" /> : <Sparkles className="w-3 h-3 text-amber-300" />}
                                AI Fix
                              </button>
                              <button
                                disabled={isCurrentResolving || aiFixingId === finding.id}
                                onClick={() => updateStatusMutation.mutate({
                                  findingId: finding.id,
                                  status: 'RESOLVED',
                                  reason: 'Resolved directly from Vulnerability Inbox'
                                })}
                                className="px-2.5 py-1 text-xs font-medium bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/20 rounded-md transition-colors flex items-center gap-1 shadow-sm"
                                title="Resolve finding then and there"
                              >
                                {isCurrentResolving ? <Loader2 className="w-3 h-3 animate-spin" /> : <Check className="w-3 h-3" />}
                                Resolve
                              </button>
                              <button
                                disabled={isCurrentResolving}
                                onClick={() => updateStatusMutation.mutate({
                                  findingId: finding.id,
                                  status: 'FALSE_POSITIVE',
                                  reason: 'Marked false positive from Inbox'
                                })}
                                className="px-2 py-1 text-xs font-medium bg-white/5 hover:bg-white/10 text-zinc-400 hover:text-zinc-200 border border-white/5 rounded-md transition-colors"
                                title="Mark as false positive"
                              >
                                FP
                              </button>
                            </>
                          ) : (
                            <button
                              disabled={isCurrentResolving}
                              onClick={() => updateStatusMutation.mutate({
                                findingId: finding.id,
                                status: 'OPEN',
                                reason: 'Reopened from Inbox'
                              })}
                              className="px-2.5 py-1 text-xs font-medium bg-zinc-800 hover:bg-zinc-700 text-zinc-300 border border-white/10 rounded-md transition-colors flex items-center gap-1"
                              title="Reopen finding"
                            >
                              {isCurrentResolving ? <Loader2 className="w-3 h-3 animate-spin" /> : <RotateCcw className="w-3 h-3" />}
                              Reopen
                            </button>
                          )}
                          
                          <button
                            onClick={() => window.location.href = `/findings/${finding.id}`}
                            className="p-1 text-zinc-500 hover:text-indigo-400 transition-colors rounded hover:bg-white/5"
                            title="View details"
                          >
                            <ChevronRight className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          
          {filteredFindings.length === 0 && (
            <div className="p-12 text-center text-zinc-500 flex flex-col items-center justify-center">
              <ShieldAlert className="w-10 h-10 mb-3 opacity-20" />
              <p>{findings.length === 0 ? 'No findings yet. Run a scan to detect vulnerabilities.' : 'No findings match the current filters.'}</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
