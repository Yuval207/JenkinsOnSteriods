"use client";

import { useQuery } from '@tanstack/react-query';
import { CheckCircle, XCircle, ChevronLeft, ShieldAlert, Layers, Timer, Loader2, RefreshCw } from 'lucide-react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { formatDistanceToNow } from 'date-fns';
import axios from 'axios';

type ScanJob = {
  id: string;
  scanner: string;
  status: string;
  findingCount: number;
  startedAt: string | null;
  completedAt: string | null;
  errorMessage: string | null;
};

type Scan = {
  id: string;
  projectId: string;
  branch: string;
  commitHash: string;
  status: string;
  progress: number;
  jobs: ScanJob[];
  summary: { totalFindings: number; critical: number; high: number; medium: number; low: number };
  createdAt: string;
  completedAt: string | null;
};

const scannerMeta: Record<string, string> = {
  SEMGREP: 'Static Application Security Testing',
  TRIVY: 'Container & Dependency Scanning',
  GITLEAKS: 'Secret Detection',
};

export default function ScanDetailsPage() {
  const { id } = useParams();

  const { data: scan, isLoading } = useQuery({
    queryKey: ['scan', id],
    queryFn: async () => {
      const res = await axios.get(`/api/v1/scans/${id}`);
      return res.data as Scan;
    },
    refetchInterval: (query) => {
      const s = query.state.data;
      return s && (s.status === 'COMPLETED' || s.status === 'FAILED') ? false : 2000;
    }
  });

  if (isLoading || !scan) {
    return <div className="flex justify-center items-center h-64"><Loader2 className="w-8 h-8 animate-spin text-zinc-500" /></div>;
  }

  const getDuration = (job: ScanJob) => {
    if (!job.startedAt || !job.completedAt) return '—';
    const ms = new Date(job.completedAt).getTime() - new Date(job.startedAt).getTime();
    return (ms / 1000).toFixed(1) + 's';
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div className="flex flex-col gap-4">
        <Link href="/scans" className="inline-flex items-center gap-2 text-xs font-medium text-zinc-500 hover:text-indigo-400 transition-colors">
          <ChevronLeft className="w-4 h-4" />
          Back to Execution History
        </Link>
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <span className={`badge text-sm px-3 py-1 uppercase tracking-wider ${
                scan.status === 'COMPLETED' ? 'badge-success' : scan.status === 'FAILED' ? 'badge-critical' : 'badge-info'
              }`}>
                {scan.status === 'RUNNING' && <RefreshCw className="w-3 h-3 animate-spin mr-1 inline" />}
                {scan.status}
              </span>
              <span className="text-zinc-500 text-sm font-mono">ID: {scan.id.substring(0, 12)}</span>
            </div>
            <h1 className="text-display text-white">Scan on {scan.branch}</h1>
            <p className="text-zinc-500 text-sm mt-1">
              Commit {scan.commitHash ? scan.commitHash.substring(0, 7) : 'latest'} · Started {formatDistanceToNow(new Date(scan.createdAt), { addSuffix: true })}
            </p>
          </div>
          <Link href={`/findings?projectId=${scan.projectId}`} className="btn-primary">
            View Triage Inbox
          </Link>
        </div>
      </div>

      {/* Hero Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mt-6">
        <div className="sg-card p-5 relative overflow-hidden group">
          <div className="absolute top-0 right-0 w-24 h-24 bg-red-500/10 rounded-full blur-2xl -mr-6 -mt-6" />
          <span className="text-[10px] text-zinc-500 uppercase tracking-wider block mb-2 relative z-10">Total Findings</span>
          <div className="text-4xl font-bold text-white tracking-tight relative z-10">{scan.summary.totalFindings}</div>
        </div>
        
        <div className="sg-card p-5 col-span-3 flex flex-col justify-center">
          <span className="text-[10px] text-zinc-500 uppercase tracking-wider block mb-4">Severity Distribution</span>
          <div className="flex items-center gap-12">
            <div className="flex flex-col">
              <span className="text-3xl font-bold text-red-500">{scan.summary.critical}</span>
              <span className="text-xs font-medium text-zinc-400">Critical</span>
            </div>
            <div className="flex flex-col">
              <span className="text-3xl font-bold text-orange-500">{scan.summary.high}</span>
              <span className="text-xs font-medium text-zinc-400">High</span>
            </div>
            <div className="flex flex-col">
              <span className="text-3xl font-bold text-yellow-500">{scan.summary.medium}</span>
              <span className="text-xs font-medium text-zinc-400">Medium</span>
            </div>
            <div className="flex flex-col">
              <span className="text-3xl font-bold text-indigo-500">{scan.summary.low}</span>
              <span className="text-xs font-medium text-zinc-400">Low</span>
            </div>
          </div>
        </div>
      </div>

      {/* Jobs */}
      <div className="sg-card mt-6">
        <div className="p-4 border-b border-white/5 bg-[#18181b]/50 flex items-center gap-2 text-sm font-medium text-white">
          <Layers className="w-4 h-4 text-indigo-400" /> Scanner Pipeline Execution
        </div>
        <div className="p-0">
          <table className="sg-table">
            <thead>
              <tr>
                <th className="w-[40px]"></th>
                <th>Engine</th>
                <th>Performance</th>
                <th>Result</th>
              </tr>
            </thead>
            <tbody>
              {scan.jobs.map((job) => (
                <tr key={job.id}>
                  <td className="pl-4">
                    {job.status === 'COMPLETED' ? (
                      <CheckCircle className="w-4 h-4 text-emerald-500" />
                    ) : job.status === 'FAILED' ? (
                      <XCircle className="w-4 h-4 text-red-500" />
                    ) : job.status === 'RUNNING' ? (
                      <RefreshCw className="w-4 h-4 text-indigo-400 animate-spin" />
                    ) : (
                      <div className="w-4 h-4 rounded-full border border-zinc-600" />
                    )}
                  </td>
                  <td>
                    <div className="font-mono text-sm text-zinc-200">{job.scanner}</div>
                    <div className="text-xs text-zinc-500 mt-0.5">{scannerMeta[job.scanner] || 'Security Scanner'}</div>
                  </td>
                  <td>
                    <div className="flex items-center gap-1.5 text-xs text-zinc-400 font-mono">
                      <Timer className="w-3.5 h-3.5 text-zinc-500" />
                      {getDuration(job)}
                    </div>
                  </td>
                  <td>
                    {job.status === 'COMPLETED' ? (
                      <span className={`badge ${job.findingCount > 0 ? 'badge-critical' : 'badge-success'}`}>
                        {job.findingCount} Identified
                      </span>
                    ) : job.status === 'FAILED' ? (
                      <span className="text-xs text-red-400 truncate max-w-[200px] block" title={job.errorMessage || ''}>
                        {job.errorMessage?.substring(0, 60) || 'Failed'}
                      </span>
                    ) : (
                      <span className="text-xs text-zinc-600">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
