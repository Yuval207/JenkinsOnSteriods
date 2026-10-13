"use client";

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ChevronLeft, Play, Activity, ShieldAlert, GitBranch, Terminal, ExternalLink, Settings2, Code2, Loader2, RefreshCw, AlertTriangle } from 'lucide-react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { formatDistanceToNow } from 'date-fns';
import axios from 'axios';

type Project = {
  id: string;
  name: string;
  repositoryUrl: string;
  defaultBranch: string;
  status: string;
  errorMessage?: string;
  detectedLanguages: string;
  detectedBuildTools: string;
  createdAt: string;
  updatedAt: string;
};

type Scan = {
  id: string;
  projectId: string;
  branch: string;
  commitHash: string;
  status: string;
  progress: number;
  summary: {
    totalFindings: number;
    critical: number;
    high: number;
    medium: number;
    low: number;
  };
  createdAt: string;
  completedAt: string | null;
};

export default function ProjectDetailsPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();

  const { data: project, isLoading: isProjectLoading } = useQuery({
    queryKey: ['project', id],
    queryFn: async () => {
      const res = await axios.get(`/api/v1/projects/${id}`);
      return res.data as Project;
    }
  });

  const { data: scansData, isLoading: isScansLoading } = useQuery({
    queryKey: ['project_scans', id],
    queryFn: async () => {
      const res = await axios.get(`/api/v1/projects/${id}/scans`);
      return res.data.content as Scan[];
    },
    refetchInterval: 3000 // Poll every 3s in case a scan is running
  });

  const syncMutation = useMutation({
    mutationFn: async () => {
      const res = await axios.post(`/api/v1/projects/${id}/sync`);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id] });
    },
    onError: (err: any) => {
      alert('Failed to sync repository: ' + (err.response?.data?.message || err.message));
    }
  });

  const triggerScanMutation = useMutation({
    mutationFn: async () => {
      const res = await axios.post(`/api/v1/projects/${id}/scans`, {
        branch: project?.defaultBranch || 'main',
        scanners: ['SEMGREP', 'TRIVY', 'GITLEAKS']
      });
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project_scans', id] });
    },
    onError: (err: any) => {
      alert('Failed to trigger scan: ' + (err.response?.data?.message || 'Repository not ready. Please sync repository first.'));
    }
  });

  if (isProjectLoading) {
    return <div className="flex justify-center items-center h-64"><Loader2 className="w-8 h-8 animate-spin text-zinc-500" /></div>;
  }

  if (!project) {
    return <div className="text-zinc-400">Project not found</div>;
  }

  const scans = scansData || [];
  const latestScan = scans.length > 0 ? scans[0] : null;

  return (
    <div className="space-y-6 pb-12">
      {/* Error banner if project sync failed */}
      {project.status === 'ERROR' && (
        <div className="p-4 bg-red-500/10 border border-red-500/30 rounded-xl flex items-center justify-between text-red-400 text-sm">
          <div className="flex items-center gap-3">
            <AlertTriangle className="w-5 h-5 text-red-400 shrink-0" />
            <div>
              <div className="font-medium text-white">Repository Sync Error</div>
              <div className="text-xs text-red-400/80 mt-0.5">{project.errorMessage || 'Authentication or network error while accessing repository.'}</div>
            </div>
          </div>
          <button
            onClick={() => syncMutation.mutate()}
            disabled={syncMutation.isPending}
            className="btn-secondary text-xs px-3 py-1.5 flex items-center gap-1.5 hover:bg-red-500/20 cursor-pointer"
          >
            {syncMutation.isPending ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <RefreshCw className="w-3.5 h-3.5" />}
            Retry Sync
          </button>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <Link href="/projects" className="inline-flex items-center gap-2 text-xs font-medium text-zinc-500 hover:text-indigo-400 transition-colors mb-3">
            <ChevronLeft className="w-4 h-4" />
            Back to Repositories
          </Link>
          <div className="flex items-center gap-3">
            <h1 className="text-display text-white">{project.name}</h1>
            <span className={`badge ${project.status === 'ACTIVE' ? 'badge-success' : 'bg-zinc-800 text-zinc-400'}`}>
              {project.status}
            </span>
          </div>
          <a href={project.repositoryUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1.5 text-zinc-400 mt-2 text-sm hover:text-indigo-400 transition-colors">
            {project.repositoryUrl} <ExternalLink className="w-3 h-3" />
          </a>
        </div>
        
        <div className="flex items-center gap-3">
          <button
            className="btn-secondary flex items-center gap-2 cursor-pointer"
            onClick={() => syncMutation.mutate()}
            disabled={syncMutation.isPending}
            title="Sync repository with remote"
          >
            {syncMutation.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin text-indigo-400" />
            ) : (
              <RefreshCw className="w-4 h-4 text-zinc-400" />
            )}
            Sync
          </button>
          <Link href={`/projects/${id}/settings`} className="btn-secondary">
            <Settings2 className="w-4 h-4" />
            Configure
          </Link>
          <button 
            className="btn-primary" 
            onClick={() => triggerScanMutation.mutate()}
            disabled={triggerScanMutation.isPending}
          >
            {triggerScanMutation.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Play className="w-4 h-4" />
            )}
            Trigger Pipeline
          </button>
        </div>
      </div>

      {/* Meta Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6 stagger-children">
        <div className="sg-card p-4">
          <div className="text-[10px] text-zinc-500 uppercase tracking-wider mb-2">Detected Languages</div>
          <div className="flex flex-wrap items-center gap-2">
            <Terminal className="w-4 h-4 text-indigo-400 shrink-0" />
            <span className="text-sm font-medium text-zinc-200 truncate">{project.detectedLanguages || 'Unknown'}</span>
          </div>
        </div>
        
        <div className="sg-card p-4">
          <div className="text-[10px] text-zinc-500 uppercase tracking-wider mb-2">Build Systems</div>
          <div className="flex flex-wrap items-center gap-2">
            <Code2 className="w-4 h-4 text-purple-400 shrink-0" />
            <span className="text-sm font-medium text-zinc-200 truncate">{project.detectedBuildTools || 'Unknown'}</span>
          </div>
        </div>
        
        <div className="sg-card p-4">
          <div className="text-[10px] text-zinc-500 uppercase tracking-wider mb-2">Target Branch</div>
          <div className="flex items-center gap-2">
            <GitBranch className="w-4 h-4 text-emerald-400" />
            <span className="text-sm font-medium text-zinc-200">{project.defaultBranch}</span>
          </div>
        </div>
        
        <div className="sg-card p-4 relative overflow-hidden group">
          <div className="absolute top-0 right-0 w-24 h-24 bg-red-500/10 rounded-full blur-2xl -mr-6 -mt-6 group-hover:bg-red-500/20 transition-colors" />
          <div className="text-[10px] text-zinc-500 uppercase tracking-wider mb-2 relative z-10">Active Risks</div>
          <div className="flex items-center gap-2 relative z-10">
            <ShieldAlert className="w-4 h-4 text-red-500" />
            <span className="text-sm font-bold text-white">{latestScan?.summary?.totalFindings || 0} Open</span>
          </div>
        </div>
      </div>

      {/* Scans list */}
      <div className="sg-card flex flex-col mt-6">
        <div className="p-4 border-b border-white/5 flex items-center justify-between bg-[#18181b]/50">
          <h2 className="text-sm font-semibold text-white flex items-center gap-2">
            Pipeline Execution History
            {isScansLoading && <Loader2 className="w-3 h-3 animate-spin text-zinc-500" />}
          </h2>
          <Link href={`/projects/${id}/scans`} className="text-xs font-medium text-indigo-400 hover:text-indigo-300">View all pipelines</Link>
        </div>
        
        <div className="overflow-x-auto">
          <table className="sg-table">
            <tbody>
              {scans.length === 0 && !isScansLoading ? (
                <tr>
                  <td colSpan={3} className="text-center py-8 text-zinc-500 text-sm">No pipeline executions yet.</td>
                </tr>
              ) : (
                scans.map((scan) => (
                  <tr key={scan.id} className="group cursor-pointer hover:bg-white/5" onClick={() => window.location.href = `/scans/${scan.id}`}>
                    <td className="w-12 text-center pl-4">
                      <div className="relative flex items-center justify-center">
                        {scan.status === 'COMPLETED' ? (
                          <div className="w-2 h-2 rounded-full bg-emerald-500" />
                        ) : scan.status === 'ERROR' ? (
                          <div className="w-2 h-2 rounded-full bg-red-500" />
                        ) : (
                          <RefreshCw className="w-3 h-3 text-indigo-400 animate-spin" />
                        )}
                      </div>
                    </td>
                    <td>
                      <div className="font-medium text-white group-hover:text-indigo-400 transition-colors">Scan {scan.id.substring(0, 8)}</div>
                      <div className="text-xs text-zinc-500 mt-0.5">Commit {scan.commitHash ? scan.commitHash.substring(0, 7) : 'latest'} on {scan.branch}</div>
                    </td>
                    <td className="text-right">
                      <div className="text-sm font-medium text-zinc-300">{formatDistanceToNow(new Date(scan.createdAt), { addSuffix: true })}</div>
                      <div className={`text-[10px] font-mono mt-0.5 ${scan.status === 'COMPLETED' ? 'text-emerald-400' : scan.status === 'ERROR' ? 'text-red-400' : 'text-indigo-400'}`}>
                        {scan.status} {scan.status === 'PENDING' && `${scan.progress}%`}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
